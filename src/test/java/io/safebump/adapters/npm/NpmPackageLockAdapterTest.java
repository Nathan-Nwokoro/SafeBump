package io.safebump.adapters.npm;

import io.safebump.core.adapter.DependencySourceException;
import io.safebump.core.model.DependencyKind;
import io.safebump.core.model.DependencySnapshot;
import io.safebump.core.model.PackageVersion;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpmPackageLockAdapterTest {

    private final NpmPackageLockAdapter adapter = new NpmPackageLockAdapter();

    @Test
    void parsesLockfileV3AndPreservesMultipleResolvedVersions() throws Exception {
        DependencySnapshot snapshot = adapter.load(fixture("package-lock.json"));
        PackageVersion root = new PackageVersion("web-app", "1.0.0");
        PackageVersion packageA = new PackageVersion("package-a", "1.0.0");
        PackageVersion packageB = new PackageVersion("package-b", "1.0.0");
        PackageVersion sharedOne = new PackageVersion("shared", "1.5.0");
        PackageVersion sharedTwo = new PackageVersion("shared", "2.1.0");

        assertEquals(root, snapshot.rootPackage());
        assertEquals(5, snapshot.graph().packageCount());
        assertEquals(5, snapshot.graph().dependencyCount());
        assertEquals(2, snapshot.findPackages("shared").size());
        assertTrue(snapshot.findPackage("shared").isEmpty());
        assertEquals(DependencyKind.DIRECT,
                snapshot.findPackage("package-a").orElseThrow().kind());
        assertEquals("git", snapshot.findPackage("package-b").orElseThrow().source());
        assertTrue(snapshot.graph().getDirectDependencies(packageA).contains(sharedOne));
        assertTrue(snapshot.graph().getDirectDependencies(packageB).contains(sharedTwo));
    }

    @Test
    void rejectsUnsupportedLockfileVersions() {
        DependencySourceException exception = assertThrows(
                DependencySourceException.class,
                () -> adapter.load(fixture("unsupported-lock.json")));

        assertTrue(exception.getMessage().contains("supports versions 2 and 3"));
    }

    private static Path fixture(String name) {
        return Path.of("src/test/resources/fixtures/npm").resolve(name);
    }
}
