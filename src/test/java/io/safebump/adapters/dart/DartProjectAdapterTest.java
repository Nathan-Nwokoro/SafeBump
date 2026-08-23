package io.safebump.adapters.dart;

import io.safebump.core.adapter.DependencySourceException;
import io.safebump.core.model.DependencySnapshot;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DartProjectAdapterTest {

    @Test
    void convertsExportedCommandOutputIntoADependencySnapshot() throws Exception {
        String json = Files.readString(fixture("valid-pub-deps.json"));
        DartProjectAdapter adapter = new DartProjectAdapter(
                ignored -> json,
                new DartPubDepsAdapter());

        DependencySnapshot snapshot = adapter.load(Path.of("example-project"));

        assertEquals("safebump_probe", snapshot.rootPackage().name());
        assertEquals(7, snapshot.graph().packageCount());
        assertEquals(8, snapshot.graph().dependencyCount());
    }

    @Test
    void identifiesInvalidJsonAsDartCommandOutput() {
        DartProjectAdapter adapter = new DartProjectAdapter(
                ignored -> "not-json",
                new DartPubDepsAdapter());

        DependencySourceException exception = assertThrows(
                DependencySourceException.class,
                () -> adapter.load(Path.of("example-project")));

        assertTrue(exception.getMessage().contains("dart pub deps --json for"));
    }

    private static Path fixture(String fileName) throws URISyntaxException {
        URL resource = Objects.requireNonNull(
                DartProjectAdapterTest.class.getResource("/fixtures/dart/" + fileName),
                "Missing test fixture " + fileName);
        return Path.of(resource.toURI());
    }
}
