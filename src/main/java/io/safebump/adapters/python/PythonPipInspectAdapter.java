package io.safebump.adapters.python;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.safebump.core.adapter.DependencySourceException;
import io.safebump.core.adapter.ProcessRunner;
import io.safebump.core.analysis.AnalysisIssue;
import io.safebump.core.analysis.ProjectAnalysis;
import io.safebump.core.analysis.ProjectAnalysisService;
import io.safebump.core.graph.DependencyGraph;
import io.safebump.core.model.DependencyKind;
import io.safebump.core.model.DependencySnapshot;
import io.safebump.core.model.PackageMetadata;
import io.safebump.core.model.PackageVersion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Builds a Python graph from pip's stable {@code inspect} JSON report. */
public final class PythonPipInspectAdapter implements ProjectAnalysisService {

    private static final Pattern REQUIREMENT_NAME = Pattern.compile(
            "^\\s*([A-Za-z0-9][A-Za-z0-9._-]*)");
    private static final Pattern PROJECT_NAME = Pattern.compile(
            "(?m)^\\s*name\\s*=\\s*[\"']([^\"']+)[\"']");
    private static final Pattern PROJECT_VERSION = Pattern.compile(
            "(?m)^\\s*version\\s*=\\s*[\"']([^\"']+)[\"']");

    private final ObjectMapper objectMapper;
    private final ProcessRunner processRunner;

    public PythonPipInspectAdapter() {
        this(new ObjectMapper(), new ProcessRunner());
    }

    PythonPipInspectAdapter(ObjectMapper objectMapper, ProcessRunner processRunner) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.processRunner = Objects.requireNonNull(processRunner, "processRunner");
    }

    @Override
    public ProjectAnalysis analyse(Path projectDirectory) throws DependencySourceException {
        Objects.requireNonNull(projectDirectory, "projectDirectory");
        Path directory = projectDirectory.toAbsolutePath().normalize();
        if (!Files.isDirectory(directory)) {
            throw new DependencySourceException(
                    "Python project directory does not exist: " + directory);
        }
        List<String> command = List.of(
                pythonExecutable(directory), "-m", "pip", "inspect", "--local");
        ProcessRunner.Result result;
        try {
            result = processRunner.run(command, directory);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DependencySourceException(
                    "Interrupted while inspecting Python dependencies in " + directory,
                    exception);
        } catch (IOException exception) {
            throw new DependencySourceException(
                    "Could not execute Python pip inspect in " + directory + ": "
                            + exception.getMessage(), exception);
        }
        if (result.exitCode() != 0) {
            String details = result.stderr().isBlank()
                    ? "Python exited with code " + result.exitCode()
                    : result.stderr().trim();
            throw new DependencySourceException(
                    "Python dependency inspection failed for " + directory + ": " + details);
        }
        return parse(directory, result.stdout());
    }

    ProjectAnalysis parse(Path projectDirectory, String json)
            throws DependencySourceException {
        JsonNode report;
        try {
            report = objectMapper.readTree(json);
        } catch (IOException exception) {
            throw new DependencySourceException(
                    "Could not parse pip inspect JSON: " + exception.getMessage(), exception);
        }
        if (!"1".equals(report.path("version").asText())) {
            throw new DependencySourceException(
                    "Unsupported pip inspect report version: "
                            + report.path("version").asText("missing"));
        }
        JsonNode installed = report.get("installed");
        if (installed == null || !installed.isArray()) {
            throw new DependencySourceException("pip inspect report has no installed array");
        }

        Map<String, Distribution> distributions = new TreeMap<>();
        for (JsonNode item : installed) {
            JsonNode metadata = item.path("metadata");
            String name = requiredText(metadata, "name");
            String version = requiredText(metadata, "version");
            String normalizedName = normalizeName(name);
            distributions.put(normalizedName, new Distribution(
                    new PackageVersion(normalizedName, version),
                    item.path("requested").asBoolean(false),
                    requirements(metadata.get("requires_dist")),
                    source(item)));
        }

        RootIdentity rootIdentity = projectIdentity(projectDirectory, distributions);
        Distribution installedRoot = distributions.get(normalizeName(rootIdentity.name()));
        PackageVersion rootPackage = installedRoot == null
                ? new PackageVersion(normalizeName(rootIdentity.name()), rootIdentity.version())
                : installedRoot.packageVersion();
        DependencyGraph graph = new DependencyGraph();
        Map<String, PackageMetadata> packages = new TreeMap<>();
        graph.addPackage(rootPackage);
        packages.put(rootPackage.toString(),
                new PackageMetadata(rootPackage, DependencyKind.ROOT, "root"));

        for (Distribution distribution : distributions.values()) {
            if (distribution.packageVersion().equals(rootPackage)) {
                continue;
            }
            graph.addPackage(distribution.packageVersion());
            packages.put(distribution.packageVersion().toString(), new PackageMetadata(
                    distribution.packageVersion(),
                    distribution.requested() ? DependencyKind.DIRECT : DependencyKind.TRANSITIVE,
                    distribution.source()));
        }

        for (Distribution distribution : distributions.values()) {
            PackageVersion from = distribution.packageVersion().equals(rootPackage)
                    ? rootPackage : distribution.packageVersion();
            for (String requirement : distribution.requirements()) {
                Distribution dependency = distributions.get(requirement);
                if (dependency != null && !dependency.packageVersion().equals(from)) {
                    graph.addDependency(from, dependency.packageVersion());
                }
            }
        }
        if (installedRoot == null) {
            distributions.values().stream()
                    .filter(Distribution::requested)
                    .filter(distribution -> !isTooling(distribution.packageVersion().name()))
                    .forEach(distribution -> graph.addDependency(
                            rootPackage, distribution.packageVersion()));
        }

        List<AnalysisIssue> issues = installedRoot == null
                ? List.of(new AnalysisIssue(
                        "PYTHON_ROOT_INFERRED",
                        "Project was not installed in the inspected environment; direct "
                                + "dependencies were inferred from pip REQUESTED metadata."))
                : List.of();
        return new ProjectAnalysis(
                new DependencySnapshot(rootPackage, graph, packages), issues, "python");
    }

    private static String pythonExecutable(Path directory) {
        List<Path> candidates = List.of(
                directory.resolve(".venv/bin/python"),
                directory.resolve("venv/bin/python"),
                directory.resolve(".venv/Scripts/python.exe"),
                directory.resolve("venv/Scripts/python.exe"));
        return candidates.stream()
                .filter(Files::isRegularFile)
                .findFirst()
                .map(Path::toString)
                .orElse("python3");
    }

    private static RootIdentity projectIdentity(
            Path projectDirectory,
            Map<String, Distribution> distributions) {
        Path pyproject = projectDirectory.resolve("pyproject.toml");
        if (Files.isRegularFile(pyproject)) {
            try {
                String text = Files.readString(pyproject);
                String name = match(PROJECT_NAME, text, projectDirectory.getFileName().toString());
                String version = match(PROJECT_VERSION, text, "0.0.0");
                return new RootIdentity(name, version);
            } catch (IOException ignored) {
                // The command already has a useful fallback for non-readable project metadata.
            }
        }
        for (Distribution distribution : distributions.values()) {
            if (distribution.source().equals("editable")) {
                return new RootIdentity(
                        distribution.packageVersion().name(),
                        distribution.packageVersion().version());
            }
        }
        return new RootIdentity(projectDirectory.getFileName().toString(), "0.0.0");
    }

    private static String match(Pattern pattern, String text, String fallback) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : fallback;
    }

    private static Set<String> requirements(JsonNode requirementsNode) {
        if (requirementsNode == null || !requirementsNode.isArray()) {
            return Set.of();
        }
        Set<String> requirements = new TreeSet<>();
        for (JsonNode value : requirementsNode) {
            if (!value.isTextual()) {
                continue;
            }
            Matcher matcher = REQUIREMENT_NAME.matcher(value.asText());
            if (matcher.find()) {
                requirements.add(normalizeName(matcher.group(1)));
            }
        }
        return Set.copyOf(requirements);
    }

    private static String source(JsonNode item) {
        JsonNode directUrl = item.get("direct_url");
        if (directUrl != null && directUrl.isObject()) {
            if (directUrl.path("dir_info").path("editable").asBoolean(false)) {
                return "editable";
            }
            return directUrl.path("url").asText("").startsWith("file:") ? "path" : "url";
        }
        String installer = item.path("installer").asText("pip").trim();
        return installer.isEmpty() ? "pip" : installer;
    }

    private static String requiredText(JsonNode node, String field)
            throws DependencySourceException {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new DependencySourceException(
                    "pip inspect package metadata is missing '" + field + "'");
        }
        return value.asText().trim();
    }

    private static String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT).replaceAll("[-_.]+", "-");
    }

    private static boolean isTooling(String name) {
        return Set.of("pip", "setuptools", "wheel").contains(name);
    }

    private record Distribution(
            PackageVersion packageVersion,
            boolean requested,
            Set<String> requirements,
            String source) {
    }

    private record RootIdentity(String name, String version) {
    }
}
