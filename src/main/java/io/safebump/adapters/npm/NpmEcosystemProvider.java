package io.safebump.adapters.npm;

import io.safebump.core.analysis.ProjectAnalysisService;
import io.safebump.core.ecosystem.DependencyEcosystemProvider;

import java.nio.file.Files;
import java.nio.file.Path;

/** Built-in npm ecosystem provider. */
public final class NpmEcosystemProvider implements DependencyEcosystemProvider {

    @Override
    public String id() {
        return "npm";
    }

    @Override
    public String displayName() {
        return "npm / Node.js";
    }

    @Override
    public boolean detects(Path projectDirectory) {
        return Files.isRegularFile(projectDirectory.resolve("package-lock.json"))
                || Files.isRegularFile(projectDirectory.resolve("npm-shrinkwrap.json"));
    }

    @Override
    public ProjectAnalysisService createAnalyzer() {
        return new NpmProjectAnalyzer();
    }
}
