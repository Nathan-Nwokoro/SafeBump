package io.safebump.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

/** SafeBump command-line application entry point. */
@Command(
        name = "safebump",
        mixinStandardHelpOptions = true,
        version = "SafeBump 0.1.0",
        description = "Understand dependency upgrades before they break your project.",
        subcommands = {GraphCommand.class, CompareCommand.class})
public final class SafeBumpApplication implements Runnable {

    @Spec
    private CommandSpec commandSpec;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new SafeBumpApplication()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        commandSpec.commandLine().usage(commandSpec.commandLine().getOut());
    }
}
