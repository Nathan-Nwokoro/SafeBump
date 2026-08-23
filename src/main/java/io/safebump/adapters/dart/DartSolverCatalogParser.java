package io.safebump.adapters.dart;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.safebump.core.adapter.DependencySourceException;
import io.safebump.core.model.PackageVersion;
import io.safebump.core.solver.PackageCandidate;
import io.safebump.core.solver.SolverProblem;
import io.safebump.core.version.SemanticVersion;
import io.safebump.core.version.VersionRange;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Parses an offline Dart candidate catalog used by the safe-upgrade solver. */
public final class DartSolverCatalogParser {

    private final ObjectMapper objectMapper;
    private final DartVersionConstraintParser constraintParser;

    public DartSolverCatalogParser() {
        this(new ObjectMapper(), new DartVersionConstraintParser());
    }

    DartSolverCatalogParser(
            ObjectMapper objectMapper,
            DartVersionConstraintParser constraintParser) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.constraintParser = Objects.requireNonNull(
                constraintParser, "constraintParser");
    }

    public SolverProblem load(Path source) throws DependencySourceException {
        Objects.requireNonNull(source, "source");
        try {
            JsonNode document = objectMapper.readTree(Files.readString(source));
            return parse(document);
        } catch (JsonProcessingException exception) {
            throw new DependencySourceException(
                    "Invalid solver catalog JSON in " + source + ": "
                            + exception.getOriginalMessage(),
                    exception);
        } catch (IOException exception) {
            throw new DependencySourceException(
                    "Unable to read solver catalog " + source + ": "
                            + exception.getMessage(),
                    exception);
        } catch (IllegalArgumentException exception) {
            throw new DependencySourceException(
                    "Invalid solver catalog data in " + source + ": "
                            + exception.getMessage(),
                    exception);
        }
    }

    SolverProblem parse(JsonNode document) {
        if (document == null || !document.isObject()) {
            throw new IllegalArgumentException("document must be an object");
        }
        PackageVersion rootVersion = parsePackageVersion(
                requiredObject(document, "root", "document"), "document.root");
        Map<String, PackageVersion> current = parseVersionMap(
                requiredObject(document, "current", "document"), "document.current");
        Map<String, PackageVersion> requested = parseVersionMap(
                requiredObject(document, "requested", "document"), "document.requested");
        Map<String, List<PackageCandidate>> candidates = parseCandidates(
                document.get("candidates"));
        PackageCandidate root = candidates
                .getOrDefault(rootVersion.name(), List.of())
                .stream()
                .filter(candidate -> candidate.packageVersion().equals(rootVersion))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "document.root must identify a candidate in document.candidates"));
        return new SolverProblem(root, current, requested, candidates);
    }

    private Map<String, List<PackageCandidate>> parseCandidates(JsonNode node) {
        if (node == null || !node.isArray()) {
            throw new IllegalArgumentException("document.candidates must be an array");
        }
        Map<String, List<PackageCandidate>> candidates = new LinkedHashMap<>();
        for (int index = 0; index < node.size(); index++) {
            JsonNode candidateNode = node.get(index);
            String context = "document.candidates[" + index + "]";
            if (!candidateNode.isObject()) {
                throw new IllegalArgumentException(context + " must be an object");
            }
            PackageVersion packageVersion = parsePackageVersion(candidateNode, context);
            Map<String, VersionRange> dependencies = parseDependencies(
                    candidateNode.get("dependencies"), context + ".dependencies");
            PackageCandidate candidate = new PackageCandidate(packageVersion, dependencies);
            List<PackageCandidate> versions = candidates.computeIfAbsent(
                    packageVersion.name(), ignored -> new ArrayList<>());
            if (versions.stream().anyMatch(existing -> existing.packageVersion()
                    .equals(packageVersion))) {
                throw new IllegalArgumentException(
                        "Duplicate candidate " + packageVersion);
            }
            versions.add(candidate);
        }
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("document.candidates must not be empty");
        }
        return candidates;
    }

    private Map<String, VersionRange> parseDependencies(JsonNode node, String context) {
        if (node == null || node.isNull()) {
            return Map.of();
        }
        if (!node.isObject()) {
            throw new IllegalArgumentException(context + " must be an object");
        }
        Map<String, VersionRange> dependencies = new LinkedHashMap<>();
        node.properties().forEach(field -> {
            if (!field.getValue().isTextual()) {
                throw new IllegalArgumentException(
                        context + "." + field.getKey() + " must be text");
            }
            dependencies.put(
                    field.getKey(), constraintParser.parse(field.getValue().textValue()));
        });
        return dependencies;
    }

    private static Map<String, PackageVersion> parseVersionMap(
            JsonNode node,
            String context) {
        Map<String, PackageVersion> versions = new LinkedHashMap<>();
        node.properties().forEach(field -> {
            if (!field.getValue().isTextual()) {
                throw new IllegalArgumentException(
                        context + "." + field.getKey() + " must be text");
            }
            versions.put(
                    field.getKey(),
                    validatedVersion(
                            field.getKey(),
                            field.getValue().textValue(),
                            context + "." + field.getKey()));
        });
        return versions;
    }

    private static PackageVersion parsePackageVersion(JsonNode node, String context) {
        return validatedVersion(
                requiredText(node, "name", context),
                requiredText(node, "version", context),
                context + ".version");
    }

    private static PackageVersion validatedVersion(
            String name,
            String version,
            String context) {
        try {
            SemanticVersion.parse(version);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    context + " is not a semantic version: " + version,
                    exception);
        }
        return new PackageVersion(name, version);
    }

    private static JsonNode requiredObject(JsonNode node, String field, String context) {
        JsonNode value = node.get(field);
        if (value == null || !value.isObject()) {
            throw new IllegalArgumentException(context + "." + field + " must be an object");
        }
        return value;
    }

    private static String requiredText(JsonNode node, String field, String context) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw new IllegalArgumentException(context + "." + field + " must be text");
        }
        return value.textValue();
    }
}
