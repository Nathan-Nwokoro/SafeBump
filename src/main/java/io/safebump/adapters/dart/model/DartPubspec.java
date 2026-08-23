package io.safebump.adapters.dart.model;

import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/** Relevant dependency metadata parsed from pubspec.yaml. */
public record DartPubspec(
        String name,
        String version,
        Optional<String> sdkConstraint,
        Map<String, DartDeclaredDependency> dependencies,
        Map<String, DartDeclaredDependency> devDependencies,
        Map<String, DartDeclaredDependency> dependencyOverrides) {

    public DartPubspec {
        name = requireText(name, "name");
        version = requireText(version, "version");
        Objects.requireNonNull(sdkConstraint, "sdkConstraint");
        sdkConstraint = sdkConstraint.map(value -> requireText(value, "sdkConstraint"));
        dependencies = immutableCopy(dependencies, DartDependencySection.MAIN);
        devDependencies = immutableCopy(devDependencies, DartDependencySection.DEV);
        dependencyOverrides = immutableCopy(
                dependencyOverrides, DartDependencySection.OVERRIDE);
    }

    public Optional<DartDeclaredDependency> findDependency(String packageName) {
        Objects.requireNonNull(packageName, "packageName");
        DartDeclaredDependency dependency = dependencies.get(packageName);
        if (dependency == null) {
            dependency = devDependencies.get(packageName);
        }
        return Optional.ofNullable(dependency);
    }

    private static Map<String, DartDeclaredDependency> immutableCopy(
            Map<String, DartDeclaredDependency> dependencies,
            DartDependencySection expectedSection) {
        Objects.requireNonNull(dependencies, "dependencies");
        NavigableMap<String, DartDeclaredDependency> copy = new TreeMap<>();
        dependencies.forEach((name, dependency) -> {
            Objects.requireNonNull(name, "dependency name");
            Objects.requireNonNull(dependency, "dependency");
            if (!name.equals(dependency.name())) {
                throw new IllegalArgumentException(
                        "Dependency key must match its package name: " + name);
            }
            if (dependency.section() != expectedSection) {
                throw new IllegalArgumentException(
                        "Dependency " + name + " must belong to " + expectedSection);
            }
            copy.put(name, dependency);
        });
        return Collections.unmodifiableNavigableMap(copy);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        String trimmedValue = value.trim();
        if (trimmedValue.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return trimmedValue;
    }
}
