package io.safebump.core.solver;

import io.safebump.core.model.PackageVersion;
import io.safebump.core.version.VersionRange;

import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/** One selectable package version and the constraints it places on dependencies. */
public record PackageCandidate(
        PackageVersion packageVersion,
        Map<String, VersionRange> dependencies) {

    public PackageCandidate {
        Objects.requireNonNull(packageVersion, "packageVersion");
        Objects.requireNonNull(dependencies, "dependencies");
        NavigableMap<String, VersionRange> copy = new TreeMap<>();
        dependencies.forEach((name, range) -> {
            Objects.requireNonNull(name, "dependency name");
            String trimmedName = name.trim();
            if (trimmedName.isEmpty()) {
                throw new IllegalArgumentException("dependency name must not be blank");
            }
            copy.put(trimmedName, Objects.requireNonNull(range, "dependency range"));
        });
        dependencies = Collections.unmodifiableNavigableMap(copy);
    }
}
