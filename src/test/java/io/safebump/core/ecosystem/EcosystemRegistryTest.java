package io.safebump.core.ecosystem;

import io.safebump.core.adapter.DependencySourceException;
import io.safebump.core.analysis.ProjectAnalysisService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EcosystemRegistryTest {

    @TempDir
    Path projectDirectory;

    @Test
    void detectsSingleMatchingProvider() throws Exception {
        Files.createFile(projectDirectory.resolve("package.lock"));
        EcosystemRegistry registry = new EcosystemRegistry(List.of(
                provider("example", "package.lock"),
                provider("other", "other.lock")));

        DetectedEcosystem detected = registry.detect(projectDirectory);

        assertEquals("example", detected.id());
        assertEquals("Example", detected.displayName());
    }

    @Test
    void requiresExplicitSelectionForAmbiguousProjectMarkers() throws Exception {
        Files.createFile(projectDirectory.resolve("one.lock"));
        Files.createFile(projectDirectory.resolve("two.lock"));
        EcosystemRegistry registry = new EcosystemRegistry(List.of(
                provider("one", "one.lock"),
                provider("two", "two.lock")));

        DependencySourceException exception = assertThrows(
                DependencySourceException.class,
                () -> registry.detect(projectDirectory));

        assertTrue(exception.getMessage().contains("one, two"));
        assertEquals("two", registry.resolve(projectDirectory, "TWO").id());
    }

    @Test
    void rejectsDuplicateProviderIds() {
        assertThrows(IllegalArgumentException.class, () -> new EcosystemRegistry(List.of(
                provider("duplicate", "one"),
                provider("DUPLICATE", "two"))));
    }

    private static DependencyEcosystemProvider provider(String id, String marker) {
        return new DependencyEcosystemProvider() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String displayName() {
                return id.substring(0, 1).toUpperCase() + id.substring(1);
            }

            @Override
            public boolean detects(Path directory) {
                return Files.isRegularFile(directory.resolve(marker));
            }

            @Override
            public ProjectAnalysisService createAnalyzer() {
                return ignored -> {
                    throw new AssertionError("Analysis is not used in detection tests");
                };
            }
        };
    }
}
