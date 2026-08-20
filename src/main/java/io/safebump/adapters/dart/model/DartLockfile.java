package io.safebump.adapters.dart.model;

import io.safebump.core.model.PackageMetadata;

import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/** Resolved package and SDK metadata parsed from pubspec.lock. */
public record DartLockfile(
        Map<String, PackageMetadata> packages,
        Map<String, String> sdkConstraints) {

    public DartLockfile {
        Objects.requireNonNull(packages, "packages");
        Objects.requireNonNull(sdkConstraints, "sdkConstraints");

        NavigableMap<String, PackageMetadata> packageCopy = new TreeMap<>();
        packages.forEach((name, metadata) -> {
            Objects.requireNonNull(name, "package name");
            Objects.requireNonNull(metadata, "package metadata");
            if (!name.equals(metadata.packageVersion().name())) {
                throw new IllegalArgumentException(
                        "Package key must match its package name: " + name);
            }
            packageCopy.put(name, metadata);
        });
        packages = Collections.unmodifiableNavigableMap(packageCopy);

        NavigableMap<String, String> sdkCopy = new TreeMap<>();
        sdkConstraints.forEach((name, constraint) -> {
            Objects.requireNonNull(name, "SDK name");
            Objects.requireNonNull(constraint, "SDK constraint");
            if (name.isBlank() || constraint.isBlank()) {
                throw new IllegalArgumentException("SDK names and constraints must not be blank");
            }
            sdkCopy.put(name.trim(), constraint.trim());
        });
        sdkConstraints = Collections.unmodifiableNavigableMap(sdkCopy);
    }

    public Optional<PackageMetadata> findPackage(String packageName) {
        Objects.requireNonNull(packageName, "packageName");
        return Optional.ofNullable(packages.get(packageName));
    }
}
