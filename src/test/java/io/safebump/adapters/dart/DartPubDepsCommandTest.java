package io.safebump.adapters.dart;

import io.safebump.core.adapter.DependencySourceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DartPubDepsCommandTest {

    @TempDir
    private Path projectDirectory;

    @BeforeEach
    void createPubspec() throws IOException {
        Files.writeString(projectDirectory.resolve("pubspec.yaml"), "name: test_project\n");
    }

    @Test
    void executesDartWithTheNormalizedProjectDirectory() throws Exception {
        AtomicReference<List<String>> capturedCommand = new AtomicReference<>();
        DartPubDepsCommand command = new DartPubDepsCommand(arguments -> {
            capturedCommand.set(arguments);
            return new DartPubDepsCommand.CommandResult(0, "{\"root\":\"test\"}", "");
        });

        String output = command.export(projectDirectory);

        assertEquals("{\"root\":\"test\"}", output);
        assertEquals(
                List.of(
                        "dart",
                        "pub",
                        "deps",
                        "--json",
                        "-C",
                        projectDirectory.toAbsolutePath().normalize().toString()),
                capturedCommand.get());
    }

    @Test
    void rejectsAMissingProjectDirectoryBeforeLaunchingDart() {
        Path missingDirectory = projectDirectory.resolve("missing");
        DartPubDepsCommand command = new DartPubDepsCommand(arguments -> {
            throw new AssertionError("Dart should not be launched");
        });

        DependencySourceException exception = assertThrows(
                DependencySourceException.class,
                () -> command.export(missingDirectory));

        assertTrue(exception.getMessage().contains("project directory does not exist"));
    }

    @Test
    void rejectsAProjectWithoutAPubspecBeforeLaunchingDart() throws IOException {
        Files.delete(projectDirectory.resolve("pubspec.yaml"));
        DartPubDepsCommand command = new DartPubDepsCommand(arguments -> {
            throw new AssertionError("Dart should not be launched");
        });

        DependencySourceException exception = assertThrows(
                DependencySourceException.class,
                () -> command.export(projectDirectory));

        assertTrue(exception.getMessage().contains("No pubspec.yaml found"));
    }

    @Test
    void reportsDartCommandFailuresWithStandardError() {
        DartPubDepsCommand command = new DartPubDepsCommand(arguments ->
                new DartPubDepsCommand.CommandResult(
                        65, "", "The pubspec.lock file is out of date."));

        DependencySourceException exception = assertThrows(
                DependencySourceException.class,
                () -> command.export(projectDirectory));

        assertTrue(exception.getMessage().contains("dependency export failed"));
        assertTrue(exception.getMessage().contains("pubspec.lock file is out of date"));
    }

    @Test
    void rejectsSuccessfulCommandsThatReturnBlankOutput() {
        DartPubDepsCommand command = new DartPubDepsCommand(arguments ->
                new DartPubDepsCommand.CommandResult(0, "  ", ""));

        DependencySourceException exception = assertThrows(
                DependencySourceException.class,
                () -> command.export(projectDirectory));

        assertTrue(exception.getMessage().contains("returned no JSON"));
    }

    @Test
    void reportsWhenDartCannotBeExecuted() {
        DartPubDepsCommand command = new DartPubDepsCommand(arguments -> {
            throw new IOException("dart executable not found");
        });

        DependencySourceException exception = assertThrows(
                DependencySourceException.class,
                () -> command.export(projectDirectory));

        assertTrue(exception.getMessage().contains("Could not execute Dart"));
        assertTrue(exception.getMessage().contains("dart executable not found"));
    }

    @Test
    void restoresTheInterruptedFlagWhenDartExecutionIsInterrupted() {
        DartPubDepsCommand command = new DartPubDepsCommand(arguments -> {
            throw new InterruptedException("interrupted");
        });

        try {
            DependencySourceException exception = assertThrows(
                    DependencySourceException.class,
                    () -> command.export(projectDirectory));

            assertTrue(exception.getMessage().contains("Interrupted while reading"));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
