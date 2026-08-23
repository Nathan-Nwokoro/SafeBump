package io.safebump.adapters.dart;

import io.safebump.core.adapter.DependencySourceException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Executes {@code dart pub deps --json} for a Dart or Flutter project. */
final class DartPubDepsCommand implements DartDependencyExporter {

    private final CommandExecutor commandExecutor;

    DartPubDepsCommand() {
        this(DartPubDepsCommand::executeProcess);
    }

    DartPubDepsCommand(CommandExecutor commandExecutor) {
        this.commandExecutor = Objects.requireNonNull(commandExecutor, "commandExecutor");
    }

    @Override
    public String export(Path projectDirectory) throws DependencySourceException {
        Objects.requireNonNull(projectDirectory, "projectDirectory");
        Path normalizedDirectory = projectDirectory.toAbsolutePath().normalize();

        if (!Files.isDirectory(normalizedDirectory)) {
            throw new DependencySourceException(
                    "Dart project directory does not exist: " + normalizedDirectory);
        }
        if (!Files.isRegularFile(normalizedDirectory.resolve("pubspec.yaml"))) {
            throw new DependencySourceException(
                    "No pubspec.yaml found in Dart project: " + normalizedDirectory);
        }

        List<String> command = List.of(
                "dart",
                "pub",
                "deps",
                "--json",
                "-C",
                normalizedDirectory.toString());

        CommandResult result;
        try {
            result = commandExecutor.execute(command);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DependencySourceException(
                    "Interrupted while reading Dart dependencies for " + normalizedDirectory,
                    exception);
        } catch (IOException exception) {
            throw new DependencySourceException(
                    "Could not execute Dart. Ensure the dart command is installed and available: "
                            + exception.getMessage(),
                    exception);
        }

        if (result.exitCode() != 0) {
            String details = result.stderr().isBlank()
                    ? "Dart exited with code " + result.exitCode()
                    : result.stderr().trim();
            throw new DependencySourceException(
                    "Dart dependency export failed for " + normalizedDirectory + ": " + details);
        }
        if (result.stdout().isBlank()) {
            throw new DependencySourceException(
                    "Dart dependency export returned no JSON for " + normalizedDirectory);
        }
        return result.stdout();
    }

    private static CommandResult executeProcess(List<String> command)
            throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).start();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<String> stdout = executor.submit(() -> readText(process.getInputStream()));
            Future<String> stderr = executor.submit(() -> readText(process.getErrorStream()));
            int exitCode = process.waitFor();
            return new CommandResult(exitCode, await(stdout), await(stderr));
        } catch (InterruptedException exception) {
            process.destroyForcibly();
            throw exception;
        }
    }

    private static String readText(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String await(Future<String> output) throws IOException, InterruptedException {
        try {
            return output.get();
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Could not read process output", cause);
        }
    }

    @FunctionalInterface
    interface CommandExecutor {
        CommandResult execute(List<String> command) throws IOException, InterruptedException;
    }

    record CommandResult(int exitCode, String stdout, String stderr) {
        CommandResult {
            Objects.requireNonNull(stdout, "stdout");
            Objects.requireNonNull(stderr, "stderr");
        }
    }
}
