package io.safebump.adapters.gradle;

import io.safebump.core.analysis.ProjectAnalysisService;
import io.safebump.core.ecosystem.DependencyEcosystemProvider;

import java.nio.file.Files;
import java.nio.file.Path;

/** Built-in Gradle ecosystem provider. */
public final class GradleEcosystemProvider implements DependencyEcosystemProvider {

    @Override
    public String id() {
        return "gradle";
    }

    @Override
    public String displayName() {
        return "Gradle";
    }

    @Override
    public boolean detects(Path projectDirectory) {
        return Files.isRegularFile(projectDirectory.resolve("settings.gradle"))
                || Files.isRegularFile(projectDirectory.resolve("settings.gradle.kts"))
                || Files.isRegularFile(projectDirectory.resolve("build.gradle"))
                || Files.isRegularFile(projectDirectory.resolve("build.gradle.kts"));
    }

    @Override
    public ProjectAnalysisService createAnalyzer() {
        return new GradleResolutionAdapter();
    }
}
