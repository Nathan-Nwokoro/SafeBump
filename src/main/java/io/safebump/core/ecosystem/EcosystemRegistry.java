package io.safebump.core.ecosystem;

import io.safebump.core.adapter.DependencySourceException;
import io.safebump.core.analysis.ProjectAnalysis;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Detects projects and routes analysis through built-in or plugin providers. */
public final class EcosystemRegistry {

    private final Map<String, DependencyEcosystemProvider> providers;

    public EcosystemRegistry(Collection<DependencyEcosystemProvider> providers) {
        Objects.requireNonNull(providers, "providers");
        Map<String, DependencyEcosystemProvider> indexed = new TreeMap<>();
        for (DependencyEcosystemProvider provider : providers) {
            Objects.requireNonNull(provider, "provider");
            String id = normalizeId(provider.id());
            DependencyEcosystemProvider previous = indexed.putIfAbsent(id, provider);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate ecosystem provider id: " + id);
            }
        }
        this.providers = Map.copyOf(indexed);
    }

    public List<DetectedEcosystem> availableEcosystems() {
        return providers.entrySet().stream()
                .map(entry -> new DetectedEcosystem(
                        entry.getKey(), entry.getValue().displayName(), entry.getValue()))
                .toList();
    }

    public DetectedEcosystem detect(Path projectDirectory)
            throws DependencySourceException {
        Path normalizedDirectory = requireDirectory(projectDirectory);
        List<DependencyEcosystemProvider> matches = providers.values().stream()
                .filter(provider -> provider.detects(normalizedDirectory))
                .toList();
        if (matches.isEmpty()) {
            throw new DependencySourceException(
                    "Could not detect a supported dependency ecosystem in "
                            + normalizedDirectory + ". Available ecosystems: "
                            + String.join(", ", providers.keySet()));
        }
        if (matches.size() > 1) {
            String ids = matches.stream()
                    .map(provider -> normalizeId(provider.id()))
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new DependencySourceException(
                    "Multiple dependency ecosystems detected in " + normalizedDirectory
                            + ": " + ids + ". Select one with --ecosystem <id>.");
        }
        DependencyEcosystemProvider provider = matches.getFirst();
        return detected(provider);
    }

    public DetectedEcosystem resolve(Path projectDirectory, String requestedId)
            throws DependencySourceException {
        if (requestedId == null || requestedId.isBlank()) {
            return detect(projectDirectory);
        }
        requireDirectory(projectDirectory);
        String normalizedId = normalizeId(requestedId);
        DependencyEcosystemProvider provider = providers.get(normalizedId);
        if (provider == null) {
            throw new DependencySourceException(
                    "Unknown ecosystem '" + requestedId + "'. Available ecosystems: "
                            + String.join(", ", providers.keySet()));
        }
        return detected(provider);
    }

    public ProjectAnalysis analyse(Path projectDirectory, String requestedId)
            throws DependencySourceException {
        DetectedEcosystem ecosystem = resolve(projectDirectory, requestedId);
        ProjectAnalysis analysis = ecosystem.provider().createAnalyzer().analyse(projectDirectory);
        return new ProjectAnalysis(
                analysis.dependencySnapshot(), analysis.issues(), ecosystem.id());
    }

    private static DetectedEcosystem detected(DependencyEcosystemProvider provider) {
        return new DetectedEcosystem(
                normalizeId(provider.id()), provider.displayName(), provider);
    }

    private static Path requireDirectory(Path projectDirectory)
            throws DependencySourceException {
        Objects.requireNonNull(projectDirectory, "projectDirectory");
        Path normalized = projectDirectory.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalized)) {
            throw new DependencySourceException(
                    "Project directory does not exist: " + normalized);
        }
        return normalized;
    }

    private static String normalizeId(String id) {
        Objects.requireNonNull(id, "id");
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9][a-z0-9._-]*")) {
            throw new IllegalArgumentException("Invalid ecosystem provider id: " + id);
        }
        return normalized;
    }
}
