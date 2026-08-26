package io.safebump.adapters.gradle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.safebump.core.adapter.DependencySourceException;
import io.safebump.core.adapter.ProcessRunner;
import io.safebump.core.analysis.ProjectAnalysis;
import io.safebump.core.analysis.ProjectAnalysisService;
import io.safebump.core.graph.DependencyGraph;
import io.safebump.core.model.DependencyKind;
import io.safebump.core.model.DependencySnapshot;
import io.safebump.core.model.PackageMetadata;
import io.safebump.core.model.PackageVersion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Exports Gradle's ResolutionResult API into SafeBump's graph model. */
public final class GradleResolutionAdapter implements ProjectAnalysisService {

    private static final String OUTPUT_MARKER = "SAFEBUMP_JSON=";
    private static final String INIT_SCRIPT = """
            import groovy.json.JsonOutput
            import org.gradle.api.artifacts.result.ResolvedDependencyResult

            gradle.projectsEvaluated {
                def candidates = ['runtimeClasspath', 'compileClasspath']
                def targetProject = rootProject.allprojects.find { project ->
                    candidates.any { name ->
                        def configuration = project.configurations.findByName(name)
                        configuration != null && configuration.canBeResolved
                    }
                }
                if (targetProject == null) {
                    targetProject = rootProject.allprojects.find { project ->
                        project.configurations.any { it.canBeResolved }
                    }
                }
                rootProject.tasks.register('safebumpDependencyGraph') {
                    doLast {
                        if (targetProject == null) {
                            throw new GradleException('No resolvable Gradle configuration found')
                        }
                        def configuration = candidates.collect {
                            targetProject.configurations.findByName(it)
                        }.find { it != null && it.canBeResolved }
                        if (configuration == null) {
                            configuration = targetProject.configurations.find { it.canBeResolved }
                        }
                        def resolution = configuration.incoming.resolutionResult
                        def resolvedRoot = resolution.rootComponent.get()
                        def nodes = [:]
                        def edges = [] as Set
                        def direct = [] as Set
                        def visited = [] as Set
                        def identify = { component ->
                            def module = component.moduleVersion
                            if (module != null) {
                                return [name: "${module.group}:${module.name}", version: module.version]
                            }
                            def group = targetProject.group?.toString()
                            if (group == null || group == '' || group == 'unspecified') {
                                group = 'gradle-project'
                            }
                            def version = targetProject.version?.toString()
                            if (version == null || version == '' || version == 'unspecified') {
                                version = '0.0.0'
                            }
                            [name: "${group}:${targetProject.name}", version: version]
                        }
                        def key = { value -> "${value.name}@${value.version}" }
                        def walk
                        walk = { component ->
                            def from = identify(component)
                            def fromKey = key(from)
                            nodes[fromKey] = from
                            component.dependencies.findAll {
                                it instanceof ResolvedDependencyResult
                            }.each { dependency ->
                                def to = identify(dependency.selected)
                                def toKey = key(to)
                                nodes[toKey] = to
                                edges << [from: fromKey, to: toKey]
                                if (component == resolvedRoot) {
                                    direct << toKey
                                }
                                if (visited.add(toKey)) {
                                    walk(dependency.selected)
                                }
                            }
                        }
                        def root = identify(resolvedRoot)
                        visited << key(root)
                        walk(resolvedRoot)
                        println('SAFEBUMP_JSON=' + JsonOutput.toJson([
                            root: key(root),
                            configuration: configuration.name,
                            nodes: nodes.collect { entry ->
                                [key: entry.key, name: entry.value.name,
                                 version: entry.value.version,
                                 direct: direct.contains(entry.key)]
                            },
                            edges: edges
                        ]))
                    }
                }
            }
            """;

    private final ObjectMapper objectMapper;
    private final ProcessRunner processRunner;

    public GradleResolutionAdapter() {
        this(new ObjectMapper(), new ProcessRunner());
    }

    GradleResolutionAdapter(ObjectMapper objectMapper, ProcessRunner processRunner) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.processRunner = Objects.requireNonNull(processRunner, "processRunner");
    }

    @Override
    public ProjectAnalysis analyse(Path projectDirectory) throws DependencySourceException {
        Objects.requireNonNull(projectDirectory, "projectDirectory");
        Path directory = projectDirectory.toAbsolutePath().normalize();
        if (!Files.isDirectory(directory)) {
            throw new DependencySourceException(
                    "Gradle project directory does not exist: " + directory);
        }
        Path initScript;
        try {
            initScript = Files.createTempFile("safebump-gradle-", ".init.gradle");
            Files.writeString(initScript, INIT_SCRIPT, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new DependencySourceException(
                    "Could not create Gradle analysis init script: "
                            + exception.getMessage(), exception);
        }
        try {
            List<String> command = List.of(
                    gradleExecutable(directory),
                    "--init-script", initScript.toString(),
                    "--quiet",
                    "safebumpDependencyGraph");
            ProcessRunner.Result result = processRunner.run(command, directory);
            if (result.exitCode() != 0) {
                String details = result.stderr().isBlank()
                        ? "Gradle exited with code " + result.exitCode()
                        : result.stderr().trim();
                throw new DependencySourceException(
                        "Gradle dependency export failed for " + directory + ": " + details);
            }
            int marker = result.stdout().lastIndexOf(OUTPUT_MARKER);
            if (marker < 0) {
                throw new DependencySourceException(
                        "Gradle dependency export returned no SafeBump JSON");
            }
            String json = result.stdout().substring(marker + OUTPUT_MARKER.length()).trim();
            return parse(json);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DependencySourceException(
                    "Interrupted while reading Gradle dependencies for " + directory,
                    exception);
        } catch (IOException exception) {
            throw new DependencySourceException(
                    "Could not execute Gradle dependency analysis for " + directory + ": "
                            + exception.getMessage(), exception);
        } finally {
            try {
                Files.deleteIfExists(initScript);
            } catch (IOException ignored) {
                // Temporary init-script cleanup is best effort.
            }
        }
    }

    ProjectAnalysis parse(String json) throws DependencySourceException {
        JsonNode report;
        try {
            report = objectMapper.readTree(json);
        } catch (IOException exception) {
            throw new DependencySourceException(
                    "Could not parse Gradle dependency JSON: " + exception.getMessage(),
                    exception);
        }
        String rootKey = requiredText(report, "root");
        JsonNode nodes = report.get("nodes");
        JsonNode edges = report.get("edges");
        if (nodes == null || !nodes.isArray() || edges == null || !edges.isArray()) {
            throw new DependencySourceException(
                    "Gradle dependency JSON requires nodes and edges arrays");
        }

        DependencyGraph graph = new DependencyGraph();
        Map<String, PackageVersion> versions = new HashMap<>();
        Map<String, PackageMetadata> metadata = new TreeMap<>();
        for (JsonNode node : nodes) {
            String key = requiredText(node, "key");
            PackageVersion version = new PackageVersion(
                    requiredText(node, "name"), requiredText(node, "version"));
            versions.put(key, version);
            graph.addPackage(version);
            DependencyKind kind = key.equals(rootKey)
                    ? DependencyKind.ROOT
                    : node.path("direct").asBoolean(false)
                            ? DependencyKind.DIRECT : DependencyKind.TRANSITIVE;
            metadata.putIfAbsent(version.toString(),
                    new PackageMetadata(version, kind, "gradle"));
        }
        PackageVersion root = versions.get(rootKey);
        if (root == null) {
            throw new DependencySourceException(
                    "Gradle dependency JSON root does not match a node: " + rootKey);
        }
        for (JsonNode edge : edges) {
            String fromKey = requiredText(edge, "from");
            String toKey = requiredText(edge, "to");
            PackageVersion from = versions.get(fromKey);
            PackageVersion to = versions.get(toKey);
            if (from == null || to == null) {
                throw new DependencySourceException(
                        "Gradle dependency edge references an unknown node");
            }
            graph.addDependency(from, to);
        }
        return new ProjectAnalysis(
                new DependencySnapshot(root, graph, metadata), List.of(), "gradle");
    }

    private static String requiredText(JsonNode node, String field)
            throws DependencySourceException {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new DependencySourceException(
                    "Gradle dependency JSON is missing '" + field + "'");
        }
        return value.asText().trim();
    }

    private static String gradleExecutable(Path directory) {
        if (Files.isRegularFile(directory.resolve("gradlew"))) {
            return directory.resolve("gradlew").toString();
        }
        if (Files.isRegularFile(directory.resolve("gradlew.bat"))) {
            return directory.resolve("gradlew.bat").toString();
        }
        return "gradle";
    }
}
