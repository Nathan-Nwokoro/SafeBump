package io.safebump.adapters.python;

import io.safebump.core.analysis.ProjectAnalysisService;
import io.safebump.core.ecosystem.DependencyEcosystemProvider;

import java.nio.file.Files;
import java.nio.file.Path;

/** Built-in Python/pip ecosystem provider. */
public final class PythonEcosystemProvider implements DependencyEcosystemProvider {

    @Override
    public String id() {
        return "python";
    }

    @Override
    public String displayName() {
        return "Python / pip";
    }

    @Override
    public boolean detects(Path projectDirectory) {
        return Files.isRegularFile(projectDirectory.resolve("pyproject.toml"))
                || Files.isRegularFile(projectDirectory.resolve("requirements.txt"))
                || Files.isRegularFile(projectDirectory.resolve("setup.py"));
    }

    @Override
    public ProjectAnalysisService createAnalyzer() {
        return new PythonPipInspectAdapter();
    }
}
