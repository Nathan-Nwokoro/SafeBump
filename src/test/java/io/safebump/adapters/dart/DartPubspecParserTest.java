package io.safebump.adapters.dart;

import io.safebump.adapters.dart.model.DartDeclaredDependency;
import io.safebump.adapters.dart.model.DartDependencySection;
import io.safebump.adapters.dart.model.DartPubspec;
import io.safebump.core.adapter.DependencySourceException;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DartPubspecParserTest {

    private final DartPubspecParser parser = new DartPubspecParser();

    @Test
    void parsesProjectIdentityAndSdkConstraint() throws Exception {
        DartPubspec pubspec = parser.load(fixture("valid-pubspec.yaml"));

        assertEquals("example_app", pubspec.name());
        assertEquals("1.2.3", pubspec.version());
        assertEquals(">=3.10.0 <4.0.0", pubspec.sdkConstraint().orElseThrow());
        assertEquals(7, pubspec.dependencies().size());
        assertEquals(1, pubspec.devDependencies().size());
        assertEquals(2, pubspec.dependencyOverrides().size());
    }

    @Test
    void parsesVersionAndSourceForms() throws Exception {
        DartPubspec pubspec = parser.load(fixture("valid-pubspec.yaml"));

        assertDependency(
                pubspec.dependencies().get("collection"),
                DartDependencySection.MAIN,
                "^1.19.1",
                "hosted");
        assertDependency(
                pubspec.dependencies().get("local_dep"),
                DartDependencySection.MAIN,
                "any",
                "path");
        assertDependency(
                pubspec.dependencies().get("git_tool"),
                DartDependencySection.MAIN,
                "any",
                "git");
        assertDependency(
                pubspec.dependencies().get("flutter"),
                DartDependencySection.MAIN,
                "any",
                "sdk");
        assertDependency(
                pubspec.dependencies().get("custom_hosted"),
                DartDependencySection.MAIN,
                ">=2.0.0 <3.0.0",
                "hosted");
        assertDependency(
                pubspec.dependencies().get("any_dep"),
                DartDependencySection.MAIN,
                "any",
                "hosted");
        assertDependency(
                pubspec.dependencies().get("empty_dep"),
                DartDependencySection.MAIN,
                "any",
                "hosted");
        assertDependency(
                pubspec.devDependencies().get("lints"),
                DartDependencySection.DEV,
                "^6.0.0",
                "hosted");
        assertDependency(
                pubspec.dependencyOverrides().get("meta"),
                DartDependencySection.OVERRIDE,
                "1.19.0",
                "hosted");
        assertDependency(
                pubspec.dependencyOverrides().get("local_dep"),
                DartDependencySection.OVERRIDE,
                "any",
                "path");
    }

    @Test
    void defaultsOptionalPubspecFields() throws Exception {
        DartPubspec pubspec = parser.load(fixture("minimal-pubspec.yaml"));

        assertEquals("0.0.0", pubspec.version());
        assertTrue(pubspec.sdkConstraint().isEmpty());
        assertTrue(pubspec.dependencies().isEmpty());
        assertTrue(pubspec.devDependencies().isEmpty());
        assertTrue(pubspec.dependencyOverrides().isEmpty());
    }

    @Test
    void rejectsAMissingPackageName() throws Exception {
        DependencySourceException exception = assertThrows(
                DependencySourceException.class,
                () -> parser.load(fixture("missing-name.yaml")));

        assertTrue(exception.getMessage().contains("document.name"));
    }

    @Test
    void rejectsDependencySectionsThatAreNotMappings() throws Exception {
        DependencySourceException exception = assertThrows(
                DependencySourceException.class,
                () -> parser.load(fixture("invalid-section.yaml")));

        assertTrue(exception.getMessage().contains("document.dependencies must be a mapping"));
    }

    @Test
    void rejectsDependenciesWithMultipleSources() throws Exception {
        DependencySourceException exception = assertThrows(
                DependencySourceException.class,
                () -> parser.load(fixture("multiple-sources.yaml")));

        assertTrue(exception.getMessage().contains("may only declare one dependency source"));
    }

    @Test
    void reportsMalformedPubspecYaml() throws Exception {
        DependencySourceException exception = assertThrows(
                DependencySourceException.class,
                () -> parser.load(fixture("malformed-yaml.yaml")));

        assertTrue(exception.getMessage().contains("Invalid Dart pubspec YAML"));
    }

    private static void assertDependency(
            DartDeclaredDependency dependency,
            DartDependencySection expectedSection,
            String expectedConstraint,
            String expectedSource) {
        assertEquals(expectedSection, dependency.section());
        assertEquals(expectedConstraint, dependency.constraint());
        assertEquals(expectedSource, dependency.source());
    }

    private static Path fixture(String fileName) throws URISyntaxException {
        URL resource = Objects.requireNonNull(
                DartPubspecParserTest.class.getResource(
                        "/fixtures/dart/pubspec/" + fileName),
                "Missing test fixture " + fileName);
        return Path.of(resource.toURI());
    }
}
