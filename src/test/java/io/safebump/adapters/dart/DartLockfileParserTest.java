package io.safebump.adapters.dart;

import io.safebump.adapters.dart.model.DartLockfile;
import io.safebump.core.adapter.DependencySourceException;
import io.safebump.core.model.DependencyKind;
import io.safebump.core.model.PackageMetadata;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DartLockfileParserTest {

    private final DartLockfileParser parser = new DartLockfileParser();

    @Test
    void parsesResolvedPackagesAndSdkConstraints() throws Exception {
        DartLockfile lockfile = parser.load(fixture("valid-pubspec.lock"));

        assertEquals(6, lockfile.packages().size());
        assertEquals(">=3.10.0 <4.0.0", lockfile.sdkConstraints().get("dart"));
        assertEquals(">=3.38.0", lockfile.sdkConstraints().get("flutter"));
        assertMetadata(lockfile, "collection", "1.19.1", DependencyKind.DIRECT, "hosted");
        assertMetadata(lockfile, "flutter", "0.0.0", DependencyKind.DIRECT, "sdk");
        assertMetadata(lockfile, "git_tool", "3.1.0", DependencyKind.DIRECT, "git");
        assertMetadata(lockfile, "lints", "6.1.0", DependencyKind.DEV, "hosted");
        assertMetadata(lockfile, "local_dep", "2.0.0", DependencyKind.DIRECT, "path");
        assertMetadata(lockfile, "meta", "1.19.0", DependencyKind.TRANSITIVE, "hosted");
        assertTrue(lockfile.findPackage("missing").isEmpty());
    }

    @Test
    void rejectsPackagesWithoutResolvedVersions() throws Exception {
        DependencySourceException exception = assertThrows(
                DependencySourceException.class,
                () -> parser.load(fixture("missing-version.lock")));

        assertTrue(exception.getMessage().contains("packages.collection.version"));
    }

    @Test
    void rejectsUnsupportedLockfileDependencyKinds() throws Exception {
        DependencySourceException exception = assertThrows(
                DependencySourceException.class,
                () -> parser.load(fixture("unsupported-kind.lock")));

        assertTrue(exception.getMessage().contains("unsupported value unexpected"));
    }

    @Test
    void rejectsPackageListsInsteadOfMappings() throws Exception {
        DependencySourceException exception = assertThrows(
                DependencySourceException.class,
                () -> parser.load(fixture("invalid-packages.lock")));

        assertTrue(exception.getMessage().contains("document.packages must be a mapping"));
    }

    @Test
    void reportsMalformedLockfileYaml() throws Exception {
        DependencySourceException exception = assertThrows(
                DependencySourceException.class,
                () -> parser.load(fixture("malformed-yaml.lock")));

        assertTrue(exception.getMessage().contains("Invalid Dart lockfile YAML"));
    }

    private static void assertMetadata(
            DartLockfile lockfile,
            String packageName,
            String expectedVersion,
            DependencyKind expectedKind,
            String expectedSource) {
        PackageMetadata metadata = lockfile.findPackage(packageName).orElseThrow();
        assertEquals(expectedVersion, metadata.packageVersion().version());
        assertEquals(expectedKind, metadata.kind());
        assertEquals(expectedSource, metadata.source());
    }

    private static Path fixture(String fileName) throws URISyntaxException {
        URL resource = Objects.requireNonNull(
                DartLockfileParserTest.class.getResource(
                        "/fixtures/dart/lockfile/" + fileName),
                "Missing test fixture " + fileName);
        return Path.of(resource.toURI());
    }
}
