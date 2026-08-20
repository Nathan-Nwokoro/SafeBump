package io.safebump.adapters.dart;

import io.safebump.core.adapter.DependencySourceAdapter;
import io.safebump.core.adapter.DependencySourceException;
import io.safebump.core.model.DependencySnapshot;

import java.nio.file.Path;
import java.util.Objects;

/** Loads a Dart project by executing Dart's machine-readable dependency export. */
public final class DartProjectAdapter implements DependencySourceAdapter {

    private final DartDependencyExporter dependencyExporter;
    private final DartPubDepsAdapter jsonAdapter;

    public DartProjectAdapter() {
        this(new DartPubDepsCommand(), new DartPubDepsAdapter());
    }

    DartProjectAdapter(
            DartDependencyExporter dependencyExporter,
            DartPubDepsAdapter jsonAdapter) {
        this.dependencyExporter = Objects.requireNonNull(
                dependencyExporter, "dependencyExporter");
        this.jsonAdapter = Objects.requireNonNull(jsonAdapter, "jsonAdapter");
    }

    @Override
    public DependencySnapshot load(Path projectDirectory) throws DependencySourceException {
        Objects.requireNonNull(projectDirectory, "projectDirectory");
        String json = dependencyExporter.export(projectDirectory);
        return jsonAdapter.loadJson(
                json,
                "dart pub deps --json for " + projectDirectory.toAbsolutePath().normalize());
    }
}
