package io.safebump.adapters.dart;

import com.fasterxml.jackson.databind.JsonNode;
import io.safebump.adapters.dart.model.DartDeclaredDependency;
import io.safebump.adapters.dart.model.DartDependencySection;
import io.safebump.adapters.dart.model.DartPubspec;
import io.safebump.core.adapter.DependencySourceException;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Parses dependency declarations and SDK constraints from pubspec.yaml. */
public final class DartPubspecParser {

    private static final Set<String> DEPENDENCY_SOURCES =
            Set.of("hosted", "git", "path", "sdk");

    public DartPubspec load(Path source) throws DependencySourceException {
        JsonNode document = DartYamlSupport.read(source, "Dart pubspec");

        try {
            String name = DartYamlSupport.requiredText(document, "name", "document");
            String version = document.hasNonNull("version")
                    ? DartYamlSupport.requiredText(document, "version", "document")
                    : "0.0.0";
            Optional<String> sdkConstraint = parseSdkConstraint(document);
            Map<String, DartDeclaredDependency> dependencies = parseSection(
                    document, "dependencies", DartDependencySection.MAIN);
            Map<String, DartDeclaredDependency> devDependencies = parseSection(
                    document, "dev_dependencies", DartDependencySection.DEV);
            Map<String, DartDeclaredDependency> dependencyOverrides = parseSection(
                    document, "dependency_overrides", DartDependencySection.OVERRIDE);

            return new DartPubspec(
                    name,
                    version,
                    sdkConstraint,
                    dependencies,
                    devDependencies,
                    dependencyOverrides);
        } catch (IllegalArgumentException exception) {
            throw new DependencySourceException(
                    "Invalid Dart pubspec data in " + source + ": "
                            + exception.getMessage(),
                    exception);
        }
    }

    private static Optional<String> parseSdkConstraint(JsonNode document) {
        JsonNode environment = document.get("environment");
        if (environment == null || environment.isNull()) {
            return Optional.empty();
        }
        if (!environment.isObject()) {
            throw new IllegalArgumentException("document.environment must be a mapping");
        }
        if (!environment.hasNonNull("sdk")) {
            return Optional.empty();
        }
        return Optional.of(DartYamlSupport.requiredText(
                environment, "sdk", "document.environment"));
    }

    private static Map<String, DartDeclaredDependency> parseSection(
            JsonNode document,
            String fieldName,
            DartDependencySection section) {
        JsonNode sectionNode = document.get(fieldName);
        if (sectionNode == null || sectionNode.isNull()) {
            return Map.of();
        }
        if (!sectionNode.isObject()) {
            throw new IllegalArgumentException(
                    "document." + fieldName + " must be a mapping");
        }

        Map<String, DartDeclaredDependency> dependencies = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> field : sectionNode.properties()) {
            String packageName = field.getKey();
            if (packageName.isBlank()) {
                throw new IllegalArgumentException(
                        "document." + fieldName + " contains a blank package name");
            }
            dependencies.put(
                    packageName,
                    parseDependency(
                            packageName,
                            field.getValue(),
                            section,
                            "document." + fieldName + "." + packageName));
        }
        return dependencies;
    }

    private static DartDeclaredDependency parseDependency(
            String packageName,
            JsonNode specification,
            DartDependencySection section,
            String context) {
        if (specification == null || specification.isNull()) {
            return new DartDeclaredDependency(packageName, section, "any", "hosted");
        }
        if (specification.isTextual()) {
            String constraint = specification.textValue().trim();
            if (constraint.isEmpty()) {
                constraint = "any";
            }
            return new DartDeclaredDependency(packageName, section, constraint, "hosted");
        }
        if (!specification.isObject()) {
            throw new IllegalArgumentException(
                    context + " must be a version scalar or source mapping");
        }

        String source = null;
        for (Map.Entry<String, JsonNode> field : specification.properties()) {
            String fieldName = field.getKey();
            if (DEPENDENCY_SOURCES.contains(fieldName)) {
                if (source != null) {
                    throw new IllegalArgumentException(
                            context + " may only declare one dependency source");
                }
                source = fieldName;
            } else if (!fieldName.equals("version")) {
                throw new IllegalArgumentException(
                        context + " contains unsupported field " + fieldName);
            }
        }

        String constraint = specification.hasNonNull("version")
                ? DartYamlSupport.requiredText(specification, "version", context)
                : "any";
        return new DartDeclaredDependency(
                packageName,
                section,
                constraint,
                source == null ? "hosted" : source);
    }
}
