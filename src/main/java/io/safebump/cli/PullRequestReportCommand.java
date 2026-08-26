package io.safebump.cli;

import io.safebump.adapters.dart.DartUpgradeAnalyzer;
import io.safebump.core.adapter.DependencySourceException;
import io.safebump.core.report.PullRequestReportFormatter;
import io.safebump.core.upgrade.UpgradeAnalysis;
import io.safebump.core.upgrade.UpgradeAnalysisService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Callable;

/** Produces a GitHub-flavoured Markdown report for a dependency update PR. */
@Command(
        name = "pr-report",
        mixinStandardHelpOptions = true,
        description = "Create a Markdown pull-request report from two resolved projects.")
public final class PullRequestReportCommand implements Callable<Integer> {

    private final UpgradeAnalysisService upgradeAnalysisService;
    private final PullRequestReportFormatter reportFormatter;

    @Parameters(
            index = "0",
            paramLabel = "<before-project>",
            description = "Resolved project directory from the pull request base.")
    private Path beforeProject;

    @Parameters(
            index = "1",
            paramLabel = "<after-project>",
            description = "Resolved project directory from the pull request head.")
    private Path afterProject;

    @Option(
            names = "--output",
            paramLabel = "<file>",
            description = "Write the Markdown report to this file instead of standard output.")
    private Path output;

    @Spec
    private CommandSpec commandSpec;

    public PullRequestReportCommand() {
        this(new DartUpgradeAnalyzer(), new PullRequestReportFormatter());
    }

    PullRequestReportCommand(
            UpgradeAnalysisService upgradeAnalysisService,
            PullRequestReportFormatter reportFormatter) {
        this.upgradeAnalysisService = Objects.requireNonNull(
                upgradeAnalysisService, "upgradeAnalysisService");
        this.reportFormatter = Objects.requireNonNull(reportFormatter, "reportFormatter");
    }

    @Override
    public Integer call() {
        try {
            UpgradeAnalysis analysis = upgradeAnalysisService.analyse(
                    beforeProject, afterProject);
            String report = reportFormatter.format(analysis);
            if (output == null) {
                commandSpec.commandLine().getOut().print(report);
            } else {
                Files.writeString(output, report, StandardCharsets.UTF_8);
            }
            return 0;
        } catch (DependencySourceException | IOException exception) {
            commandSpec.commandLine().getErr().println("Error: " + exception.getMessage());
            return 2;
        }
    }
}
