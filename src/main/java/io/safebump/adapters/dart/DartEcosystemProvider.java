package io.safebump.adapters.dart;

import io.safebump.core.analysis.ProjectAnalysisService;
import io.safebump.core.ecosystem.DependencyEcosystemProvider;

import java.nio.file.Files;
import java.nio.file.Path;

/** Built-in Dart and Flutter ecosystem provider. */
public final class DartEcosystemProvider implements DependencyEcosystemProvider {

    @Override
    public String id() {
        return "dart";
    }

    @Override
    public String displayName() {
        return "Dart / Flutter";
    }

    @Override
    public boolean detects(Path projectDirectory) {
        return Files.isRegularFile(projectDirectory.resolve("pubspec.yaml"));
    }

    @Override
    public ProjectAnalysisService createAnalyzer() {
        return new DartProjectAnalyzer();
    }
}
