package io.safebump.adapters.maven;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Builds a Maven graph from dependency-plugin JSON output. */
public final class MavenDependencyTreeAdapter implements ProjectAnalysisService {

    private static final String DEPENDENCY_GOAL =
            "org.apache.maven.plugins:maven-dependency-plugin:3.10.0:tree";
    private final ObjectMapper objectMapper;
    private final ProcessRunner processRunner;

    public MavenDependencyTreeAdapter() {
        this(new ObjectMapper(), new ProcessRunner());
    }

    MavenDependencyTreeAdapter(ObjectMapper objectMapper, ProcessRunner processRunner) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.processRunner = Objects.requireNonNull(processRunner, "processRunner");
    }

    @Override
    public ProjectAnalysis analyse(Path projectDirectory) throws DependencySourceException {
        Objects.requireNonNull(projectDirectory, "projectDirectory");
        Path directory = projectDirectory.toAbsolutePath().normalize();
        if (!Files.isRegularFile(directory.resolve("pom.xml"))) {
            throw new DependencySourceException(
                    "No pom.xml found in Maven project: " + directory);
        }
        Path output;
        try {
            output = Files.createTempFile("safebump-maven-tree-", ".json");
        } catch (IOException exception) {
            throw new DependencySourceException(
                    "Could not create Maven dependency report file: "
                            + exception.getMessage(), exception);
        }
        try {
            List<String> command = List.of(
                    mavenExecutable(directory),
                    "--batch-mode",
                    "-Dstyle.color=never",
                    DEPENDENCY_GOAL,
                    "-DoutputType=json",
                    "-DoutputFile=" + output);
            ProcessRunner.Result result = processRunner.run(command, directory);
            if (result.exitCode() != 0) {
                String details = result.stderr().isBlank()
                        ? "Maven exited with code " + result.exitCode()
                        : result.stderr().trim();
                throw new DependencySourceException(
                        "Maven dependency export failed for " + directory + ": " + details);
            }
            return parse(Files.readString(output));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DependencySourceException(
                    "Interrupted while reading Maven dependencies for " + directory,
                    exception);
        } catch (IOException exception) {
            throw new DependencySourceException(
                    "Could not execute Maven dependency analysis for " + directory + ": "
                            + exception.getMessage(), exception);
        } finally {
            try {
                Files.deleteIfExists(output);
            } catch (IOException ignored) {
                // Temporary report cleanup is best effort.
            }
        }
    }

    ProjectAnalysis parse(String json) throws DependencySourceException {
        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (IOException exception) {
            throw new DependencySourceException(
                    "Could not parse Maven dependency JSON: " + exception.getMessage(),
                    exception);
        }
        DependencyGraph graph = new DependencyGraph();
        Map<String, PackageMetadata> metadata = new TreeMap<>();
        PackageVersion rootPackage = packageVersion(root);
        visit(root, null, 0, graph, metadata);
        return new ProjectAnalysis(
                new DependencySnapshot(rootPackage, graph, metadata), List.of(), "maven");
    }

    private static void visit(
            JsonNode node,
            PackageVersion parent,
            int depth,
            DependencyGraph graph,
            Map<String, PackageMetadata> metadata) throws DependencySourceException {
        PackageVersion current = packageVersion(node);
        DependencyKind kind = depth == 0
                ? DependencyKind.ROOT
                : depth == 1 && "test".equals(node.path("scope").asText())
                        ? DependencyKind.DEV
                        : depth == 1 ? DependencyKind.DIRECT : DependencyKind.TRANSITIVE;
        graph.addPackage(current);
        metadata.putIfAbsent(current.toString(), new PackageMetadata(
                current, kind, "maven:" + node.path("scope").asText("compile")));
        if (parent != null) {
            graph.addDependency(parent, current);
        }
        JsonNode children = node.get("children");
        if (children != null && children.isArray()) {
            for (JsonNode child : children) {
                visit(child, current, depth + 1, graph, metadata);
            }
        }
    }

    private static PackageVersion packageVersion(JsonNode node)
            throws DependencySourceException {
        String groupId = requiredText(node, "groupId");
        String artifactId = requiredText(node, "artifactId");
        String version = requiredText(node, "version");
        return new PackageVersion(groupId + ":" + artifactId, version);
    }

    private static String requiredText(JsonNode node, String field)
            throws DependencySourceException {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new DependencySourceException(
                    "Maven dependency node is missing '" + field + "'");
        }
        return value.asText().trim();
    }

    private static String mavenExecutable(Path directory) {
        if (Files.isRegularFile(directory.resolve("mvnw"))) {
            return directory.resolve("mvnw").toString();
        }
        if (Files.isRegularFile(directory.resolve("mvnw.cmd"))) {
            return directory.resolve("mvnw.cmd").toString();
        }
        return "mvn";
    }
}
