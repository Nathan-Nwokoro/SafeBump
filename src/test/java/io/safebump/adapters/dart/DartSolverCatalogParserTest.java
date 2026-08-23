package io.safebump.adapters.dart;

import io.safebump.core.adapter.DependencySourceException;
import io.safebump.core.solver.SolverProblem;
import io.safebump.core.version.SemanticVersion;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DartSolverCatalogParserTest {

    private final DartSolverCatalogParser parser = new DartSolverCatalogParser();

    @Test
    void parsesCandidatesCurrentVersionsRequestsAndConstraints() throws Exception {
        SolverProblem problem = parser.load(fixture("valid-catalog.json"));

        assertEquals("app@1.0.0", problem.root().packageVersion().toString());
        assertEquals(4, problem.currentVersions().size());
        assertEquals("package-a@2.0.0",
                problem.requestedVersions().get("package-a").toString());
        assertEquals(4, problem.candidatesByPackage().size());
        assertEquals(2, problem.candidatesByPackage().get("package-a").size());
        assertTrue(problem.candidatesByPackage().get("package-a").get(1)
                .dependencies().get("core-lib")
                .contains(SemanticVersion.parse("5.0.0")));
    }

    @Test
    void rejectsAMissingRootCandidate() throws Exception {
        DependencySourceException exception = assertThrows(
                DependencySourceException.class,
                () -> parser.load(fixture("missing-root-candidate.json")));

        assertTrue(exception.getMessage().contains("document.root"));
    }

    @Test
    void reportsMalformedJsonAndUnreadableFiles() throws Exception {
        assertThrows(
                DependencySourceException.class,
                () -> parser.load(fixture("malformed-catalog.json")));
        assertThrows(
                DependencySourceException.class,
                () -> parser.load(Path.of("does-not-exist.json")));
    }

    @Test
    void rejectsNonSemanticCandidateVersions() throws Exception {
        DependencySourceException exception = assertThrows(
                DependencySourceException.class,
                () -> parser.load(fixture("invalid-version-catalog.json")));

        assertTrue(exception.getMessage().contains("not a semantic version"));
    }

    private static Path fixture(String fileName) throws URISyntaxException {
        URL resource = Objects.requireNonNull(
                DartSolverCatalogParserTest.class.getResource(
                        "/fixtures/dart/solver/" + fileName),
                "Missing solver fixture " + fileName);
        return Path.of(resource.toURI());
    }
}
