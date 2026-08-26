package io.safebump.cli;

import io.safebump.adapters.BuiltInEcosystems;
import io.safebump.core.adapter.DependencySourceException;
import io.safebump.core.ecosystem.DetectedEcosystem;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.nio.file.Path;
import java.util.concurrent.Callable;

/** Detects which dependency ecosystem owns a project directory. */
@Command(
        name = "detect",
        mixinStandardHelpOptions = true,
        description = "Detect a project's dependency ecosystem.")
public final class DetectCommand implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "<project>")
    private Path projectDirectory;

    @Option(names = "--ecosystem", paramLabel = "<id>",
            description = "Select an ecosystem when project markers are ambiguous.")
    private String ecosystem;

    @Option(names = "--plugin-dir", paramLabel = "<directory>",
            description = "Load additional ecosystem-provider JARs from this directory.")
    private Path pluginDirectory;

    @Spec
    private CommandSpec commandSpec;

    @Override
    public Integer call() {
        try {
            DetectedEcosystem detected = BuiltInEcosystems.create(pluginDirectory)
                    .resolve(projectDirectory, ecosystem);
            commandSpec.commandLine().getOut().printf(
                    "Detected ecosystem: %s (%s)%n", detected.id(), detected.displayName());
            return 0;
        } catch (DependencySourceException exception) {
            commandSpec.commandLine().getErr().println("Error: " + exception.getMessage());
            return 2;
        }
    }
}
