package io.safebump.adapters.maven;

import io.safebump.core.analysis.ProjectAnalysisService;
import io.safebump.core.ecosystem.DependencyEcosystemProvider;

import java.nio.file.Files;
import java.nio.file.Path;

/** Built-in Maven ecosystem provider. */
public final class MavenEcosystemProvider implements DependencyEcosystemProvider {

    @Override
    public String id() {
        return "maven";
    }

    @Override
    public String displayName() {
        return "Apache Maven";
    }

    @Override
    public boolean detects(Path projectDirectory) {
        return Files.isRegularFile(projectDirectory.resolve("pom.xml"));
    }

    @Override
    public ProjectAnalysisService createAnalyzer() {
        return new MavenDependencyTreeAdapter();
    }
}
