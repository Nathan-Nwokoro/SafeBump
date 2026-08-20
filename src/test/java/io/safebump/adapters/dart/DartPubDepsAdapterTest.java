package io.safebump.adapters.dart;

import io.safebump.core.adapter.DependencySourceException;
import io.safebump.core.model.DependencyKind;
import io.safebump.core.model.DependencySnapshot;
import io.safebump.core.model.PackageMetadata;
import io.safebump.core.model.PackageVersion;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DartPubDepsAdapterTest {

    private final DartPubDepsAdapter adapter = new DartPubDepsAdapter();

    @Test
    void convertsDartDependencyJsonIntoTheCoreGraph() throws Exception {
        DependencySnapshot snapshot = adapter.load(fixture("valid-pub-deps.json"));
        PackageVersion rootPackage = packageVersion("safebump_probe", "1.0.0");

        assertEquals(rootPackage, snapshot.rootPackage());
        assertEquals(7, snapshot.graph().packageCount());
        assertEquals(8, snapshot.graph().dependencyCount());
        assertEquals(
                List.of(
                        packageVersion("collection", "1.19.1"),
                        packageVersion("flutter", "0.0.0"),
                        packageVersion("git_tool", "3.1.0"),
                        packageVersion("lints", "6.1.0"),
                        packageVersion("local_dep", "2.0.0")),
                List.copyOf(snapshot.graph().getDirectDependencies(rootPackage)));
        assertEquals(6, snapshot.graph().getTransitiveDependencies(rootPackage).size());
        assertFalse(snapshot.graph().hasCycle());
    }

    @Test
    void preservesDependencyKindsAndSources() throws Exception {
        DependencySnapshot snapshot = adapter.load(fixture("valid-pub-deps.json"));

        assertMetadata(snapshot, "safebump_probe", DependencyKind.ROOT, "root");
        assertMetadata(snapshot, "collection", DependencyKind.DIRECT, "hosted");
        assertMetadata(snapshot, "local_dep", DependencyKind.DIRECT, "path");
        assertMetadata(snapshot, "git_tool", DependencyKind.DIRECT, "git");
        assertMetadata(snapshot, "flutter", DependencyKind.DIRECT, "sdk");
        assertMetadata(snapshot, "lints", DependencyKind.DEV, "hosted");
        assertMetadata(snapshot, "meta", DependencyKind.TRANSITIVE, "hosted");
        assertTrue(snapshot.findPackage("missing").isEmpty());
    }

    @Test
    void reportsMalformedJson() throws Exception {
        DependencySourceException exception = assertThrows(
                DependencySourceException.class,
                () -> adapter.load(fixture("malformed-json.json")));

        assertTrue(exception.getMessage().contains("Invalid Dart dependency JSON"));
    }

    @Test
    void reportsMissingRequiredPackageFields() throws Exception {
        DependencySourceException exception = assertThrows(
                DependencySourceException.class,
                () -> adapter.load(fixture("missing-version.json")));

        assertTrue(exception.getMessage().contains("packages[0].version"));
    }

    @Test
    void reportsReferencesToMissingPackages() throws Exception {
        DependencySourceException exception = assertThrows(
                DependencySourceException.class,
                () -> adapter.load(fixture("missing-dependency.json")));

        assertTrue(exception.getMessage().contains(
                "package broken references missing dependency not_in_packages"));
    }

    @Test
    void rejectsDuplicatePackageNames() throws Exception {
        DependencySourceException exception = assertThrows(
                DependencySourceException.class,
                () -> adapter.load(fixture("duplicate-package.json")));

        assertTrue(exception.getMessage().contains("duplicate package duplicate"));
    }

    @Test
    void rejectsUnsupportedDependencyKinds() throws Exception {
        DependencySourceException exception = assertThrows(
                DependencySourceException.class,
                () -> adapter.load(fixture("unsupported-kind.json")));

        assertTrue(exception.getMessage().contains("unsupported value unexpected"));
    }

    @Test
    void reportsUnreadableInputFiles() {
        Path missingFile = Path.of("does-not-exist", "dart-pub-deps.json");

        DependencySourceException exception = assertThrows(
                DependencySourceException.class,
                () -> adapter.load(missingFile));

        assertTrue(exception.getMessage().contains(
                "Could not read Dart dependency JSON from"));
    }

    private static void assertMetadata(
            DependencySnapshot snapshot,
            String packageName,
            DependencyKind expectedKind,
            String expectedSource) {
        PackageMetadata metadata = snapshot.findPackage(packageName).orElseThrow();
        assertEquals(expectedKind, metadata.kind());
        assertEquals(expectedSource, metadata.source());
    }

    private static PackageVersion packageVersion(String name, String version) {
        return new PackageVersion(name, version);
    }

    private static Path fixture(String fileName) throws URISyntaxException {
        URL resource = Objects.requireNonNull(
                DartPubDepsAdapterTest.class.getResource("/fixtures/dart/" + fileName),
                "Missing test fixture " + fileName);
        return Path.of(resource.toURI());
    }
}
