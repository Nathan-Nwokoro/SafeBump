package io.safebump.adapters.dart;

import com.fasterxml.jackson.databind.JsonNode;
import io.safebump.adapters.dart.model.DartLockfile;
import io.safebump.core.adapter.DependencySourceException;
import io.safebump.core.model.DependencyKind;
import io.safebump.core.model.PackageMetadata;
import io.safebump.core.model.PackageVersion;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Parses resolved package and SDK metadata from pubspec.lock. */
public final class DartLockfileParser {

    public DartLockfile load(Path source) throws DependencySourceException {
        JsonNode document = DartYamlSupport.read(source, "Dart lockfile");

        try {
            return new DartLockfile(parsePackages(document), parseSdkConstraints(document));
        } catch (IllegalArgumentException exception) {
            throw new DependencySourceException(
                    "Invalid Dart lockfile data in " + source + ": "
                            + exception.getMessage(),
                    exception);
        }
    }

    private static Map<String, PackageMetadata> parsePackages(JsonNode document) {
        JsonNode packagesNode = document.get("packages");
        if (packagesNode == null || !packagesNode.isObject()) {
            throw new IllegalArgumentException("document.packages must be a mapping");
        }

        Map<String, PackageMetadata> packages = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> field : packagesNode.properties()) {
            String packageName = field.getKey();
            JsonNode packageNode = field.getValue();
            String context = "document.packages." + packageName;
            if (!packageNode.isObject()) {
                throw new IllegalArgumentException(context + " must be a mapping");
            }

            String version = DartYamlSupport.requiredText(packageNode, "version", context);
            String source = DartYamlSupport.requiredText(packageNode, "source", context);
            String dependency = DartYamlSupport.requiredText(
                    packageNode, "dependency", context);
            DependencyKind kind = parseDependencyKind(dependency, context);
            PackageVersion packageVersion = new PackageVersion(packageName, version);
            packages.put(
                    packageName,
                    new PackageMetadata(packageVersion, kind, source));
        }
        return packages;
    }

    private static Map<String, String> parseSdkConstraints(JsonNode document) {
        JsonNode sdksNode = document.get("sdks");
        if (sdksNode == null || sdksNode.isNull()) {
            return Map.of();
        }
        if (!sdksNode.isObject()) {
            throw new IllegalArgumentException("document.sdks must be a mapping");
        }

        Map<String, String> sdkConstraints = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> field : sdksNode.properties()) {
            JsonNode constraint = field.getValue();
            if (!constraint.isTextual() || constraint.textValue().isBlank()) {
                throw new IllegalArgumentException(
                        "document.sdks." + field.getKey()
                                + " must be a non-blank scalar");
            }
            sdkConstraints.put(field.getKey(), constraint.textValue().trim());
        }
        return sdkConstraints;
    }

    private static DependencyKind parseDependencyKind(String value, String context) {
        return switch (value) {
            case "direct main" -> DependencyKind.DIRECT;
            case "direct dev" -> DependencyKind.DEV;
            case "transitive" -> DependencyKind.TRANSITIVE;
            default -> throw new IllegalArgumentException(
                    context + ".dependency has unsupported value " + value);
        };
    }
}
