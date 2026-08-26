package io.safebump.core.ecosystem;

import io.safebump.core.analysis.ProjectAnalysisService;

import java.nio.file.Files;
import java.nio.file.Path;

/** ServiceLoader fixture used to verify external provider discovery. */
public final class TestPluginProvider implements DependencyEcosystemProvider {

    @Override
    public String id() {
        return "test-plugin";
    }

    @Override
    public String displayName() {
        return "Test plugin";
    }

    @Override
    public boolean detects(Path projectDirectory) {
        return Files.isRegularFile(projectDirectory.resolve("test.plugin"));
    }

    @Override
    public ProjectAnalysisService createAnalyzer() {
        return ignored -> {
            throw new AssertionError("Plugin analysis is not used by the loader test");
        };
    }
}
