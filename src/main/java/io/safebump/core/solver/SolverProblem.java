package io.safebump.core.solver;

import io.safebump.core.model.PackageVersion;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/** Candidate repository, current resolution, and exact versions requested by a user. */
public record SolverProblem(
        PackageCandidate root,
        Map<String, PackageVersion> currentVersions,
        Map<String, PackageVersion> requestedVersions,
        Map<String, List<PackageCandidate>> candidatesByPackage) {

    public SolverProblem {
        Objects.requireNonNull(root, "root");
        currentVersions = immutableVersions(currentVersions, "currentVersions");
        requestedVersions = immutableVersions(requestedVersions, "requestedVersions");
        candidatesByPackage = immutableCandidates(candidatesByPackage);

        String rootName = root.packageVersion().name();
        PackageVersion currentRoot = currentVersions.get(rootName);
        if (currentRoot == null || !currentRoot.equals(root.packageVersion())) {
            throw new IllegalArgumentException(
                    "Current versions must contain the exact root candidate");
        }
        List<PackageCandidate> rootCandidates = candidatesByPackage.get(rootName);
        if (rootCandidates == null || !rootCandidates.contains(root)) {
            throw new IllegalArgumentException(
                    "Candidate repository must contain the root candidate");
        }
    }

    private static Map<String, PackageVersion> immutableVersions(
            Map<String, PackageVersion> versions,
            String fieldName) {
        Objects.requireNonNull(versions, fieldName);
        NavigableMap<String, PackageVersion> copy = new TreeMap<>();
        versions.forEach((name, version) -> {
            Objects.requireNonNull(name, fieldName + " name");
            Objects.requireNonNull(version, fieldName + " version");
            if (!name.equals(version.name())) {
                throw new IllegalArgumentException(
                        fieldName + " key must match package name: " + name);
            }
            copy.put(name, version);
        });
        return Collections.unmodifiableNavigableMap(copy);
    }

    private static Map<String, List<PackageCandidate>> immutableCandidates(
            Map<String, List<PackageCandidate>> candidates) {
        Objects.requireNonNull(candidates, "candidatesByPackage");
        NavigableMap<String, List<PackageCandidate>> copy = new TreeMap<>();
        candidates.forEach((name, versions) -> {
            Objects.requireNonNull(name, "candidate package name");
            Objects.requireNonNull(versions, "candidate versions");
            List<PackageCandidate> versionCopy = versions.stream()
                    .map(candidate -> Objects.requireNonNull(candidate, "candidate"))
                    .peek(candidate -> {
                        if (!name.equals(candidate.packageVersion().name())) {
                            throw new IllegalArgumentException(
                                    "Candidate key must match package name: " + name);
                        }
                    })
                    .distinct()
                    .toList();
            if (versionCopy.isEmpty()) {
                throw new IllegalArgumentException(
                        "Candidate list must not be empty: " + name);
            }
            if (versionCopy.stream()
                    .map(PackageCandidate::packageVersion)
                    .collect(java.util.stream.Collectors.toCollection(HashSet::new))
                    .size() != versionCopy.size()) {
                throw new IllegalArgumentException(
                        "Candidate versions must be unique: " + name);
            }
            copy.put(name, versionCopy);
        });
        return Collections.unmodifiableNavigableMap(copy);
    }
}
