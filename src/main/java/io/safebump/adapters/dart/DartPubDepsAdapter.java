package io.safebump.adapters.dart;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.safebump.core.adapter.DependencySourceAdapter;
import io.safebump.core.adapter.DependencySourceException;
import io.safebump.core.graph.DependencyGraph;
import io.safebump.core.model.DependencyKind;
import io.safebump.core.model.DependencySnapshot;
import io.safebump.core.model.PackageMetadata;
import io.safebump.core.model.PackageVersion;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Parses the JSON emitted by {@code dart pub deps --json}. */
public final class DartPubDepsAdapter implements DependencySourceAdapter {

    private final ObjectMapper objectMapper;

    public DartPubDepsAdapter() {
        this(new ObjectMapper());
    }

    DartPubDepsAdapter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public DependencySnapshot load(Path source) throws DependencySourceException {
        Objects.requireNonNull(source, "source");

        try (Reader reader = Files.newBufferedReader(source)) {
            return load(reader, source.toString());
        } catch (IOException exception) {
            throw new DependencySourceException(
                    "Could not read Dart dependency JSON from " + source + ": "
                            + exception.getMessage(),
                    exception);
        }

    }

    DependencySnapshot loadJson(String json, String sourceDescription)
            throws DependencySourceException {
        Objects.requireNonNull(json, "json");
        Objects.requireNonNull(sourceDescription, "sourceDescription");
        return load(new StringReader(json), sourceDescription);
    }

    private DependencySnapshot load(Reader reader, String sourceDescription)
            throws DependencySourceException {
        try {
            JsonNode document = objectMapper.readTree(reader);
            return parseDocument(document);
        } catch (JsonProcessingException exception) {
            throw new DependencySourceException(
                    "Invalid Dart dependency JSON in " + sourceDescription + ": "
                            + exception.getOriginalMessage(),
                    exception);
        } catch (IOException exception) {
            throw new DependencySourceException(
                    "Could not read Dart dependency JSON from " + sourceDescription + ": "
                            + exception.getMessage(),
                    exception);
        } catch (IllegalArgumentException exception) {
            throw new DependencySourceException(
                    "Invalid Dart dependency data in " + sourceDescription + ": "
                            + exception.getMessage(),
                    exception);
        }
    }

    private DependencySnapshot parseDocument(JsonNode document) {
        if (document == null || !document.isObject()) {
            throw new IllegalArgumentException("document must be a JSON object");
        }

        String rootPackageName = requiredText(document, "root", "document");
        JsonNode packagesNode = document.get("packages");
        if (packagesNode == null || !packagesNode.isArray()) {
            throw new IllegalArgumentException("document.packages must be an array");
        }

        Map<String, ParsedPackage> parsedPackages = new LinkedHashMap<>();
        for (int index = 0; index < packagesNode.size(); index++) {
            ParsedPackage parsedPackage = parsePackage(packagesNode.get(index), index);
            String packageName = parsedPackage.metadata().packageVersion().name();
            if (parsedPackages.putIfAbsent(packageName, parsedPackage) != null) {
                throw new IllegalArgumentException(
                        "document.packages contains duplicate package " + packageName);
            }
        }

        if (parsedPackages.isEmpty()) {
            throw new IllegalArgumentException("document.packages must not be empty");
        }

        ParsedPackage rootPackage = parsedPackages.get(rootPackageName);
        if (rootPackage == null) {
            throw new IllegalArgumentException(
                    "root package " + rootPackageName + " is missing from document.packages");
        }
        if (rootPackage.metadata().kind() != DependencyKind.ROOT) {
            throw new IllegalArgumentException(
                    "root package " + rootPackageName + " must have kind root");
        }

        DependencyGraph graph = new DependencyGraph();
        Map<String, PackageMetadata> metadataByName = new LinkedHashMap<>();
        parsedPackages.forEach((name, parsedPackage) -> {
            PackageMetadata metadata = parsedPackage.metadata();
            graph.addPackage(metadata.packageVersion());
            metadataByName.put(name, metadata);
        });

        parsedPackages.forEach((name, parsedPackage) -> {
            PackageVersion from = parsedPackage.metadata().packageVersion();
            for (String dependencyName : parsedPackage.dependencies()) {
                ParsedPackage dependency = parsedPackages.get(dependencyName);
                if (dependency == null) {
                    throw new IllegalArgumentException(
                            "package " + name + " references missing dependency "
                                    + dependencyName);
                }
                graph.addDependency(from, dependency.metadata().packageVersion());
            }
        });

        return new DependencySnapshot(
                rootPackage.metadata().packageVersion(), graph, metadataByName);
    }

    private ParsedPackage parsePackage(JsonNode packageNode, int index) {
        String context = "document.packages[" + index + "]";
        if (packageNode == null || !packageNode.isObject()) {
            throw new IllegalArgumentException(context + " must be an object");
        }

        String name = requiredText(packageNode, "name", context);
        String version = requiredText(packageNode, "version", context);
        String kindValue = requiredText(packageNode, "kind", context);
        String source = requiredText(packageNode, "source", context);
        List<String> dependencies = requiredStringArray(packageNode, "dependencies", context);

        DependencyKind kind;
        try {
            kind = DependencyKind.valueOf(kindValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    context + ".kind has unsupported value " + kindValue);
        }

        PackageVersion packageVersion = new PackageVersion(name, version);
        return new ParsedPackage(
                new PackageMetadata(packageVersion, kind, source), dependencies);
    }

    private static String requiredText(JsonNode object, String fieldName, String context) {
        JsonNode value = object.get(fieldName);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw new IllegalArgumentException(
                    context + "." + fieldName + " must be a non-blank string");
        }
        return value.textValue().trim();
    }

    private static List<String> requiredStringArray(
            JsonNode object,
            String fieldName,
            String context) {
        JsonNode values = object.get(fieldName);
        if (values == null || !values.isArray()) {
            throw new IllegalArgumentException(
                    context + "." + fieldName + " must be an array");
        }

        List<String> result = new ArrayList<>();
        for (int index = 0; index < values.size(); index++) {
            JsonNode value = values.get(index);
            if (!value.isTextual() || value.textValue().isBlank()) {
                throw new IllegalArgumentException(
                        context + "." + fieldName + "[" + index
                                + "] must be a non-blank string");
            }
            result.add(value.textValue().trim());
        }
        return List.copyOf(result);
    }

    private record ParsedPackage(
            PackageMetadata metadata,
            List<String> dependencies) {
    }
}
