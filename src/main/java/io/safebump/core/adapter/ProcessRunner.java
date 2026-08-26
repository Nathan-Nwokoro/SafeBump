package io.safebump.core.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Runs package-manager commands without allowing stdout/stderr pipe deadlocks. */
public class ProcessRunner {

    public Result run(List<String> command, Path workingDirectory)
            throws IOException, InterruptedException {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(workingDirectory, "workingDirectory");
        Process process = new ProcessBuilder(command)
                .directory(workingDirectory.toFile())
                .start();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<String> stdout = executor.submit(() -> read(process.getInputStream()));
            Future<String> stderr = executor.submit(() -> read(process.getErrorStream()));
            int exitCode = process.waitFor();
            return new Result(exitCode, await(stdout), await(stderr));
        } catch (InterruptedException exception) {
            process.destroyForcibly();
            throw exception;
        }
    }

    private static String read(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String await(Future<String> future)
            throws IOException, InterruptedException {
        try {
            return future.get();
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Could not read process output", cause);
        }
    }

    public record Result(int exitCode, String stdout, String stderr) {
        public Result {
            Objects.requireNonNull(stdout, "stdout");
            Objects.requireNonNull(stderr, "stderr");
        }
    }
}
