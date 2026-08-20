package io.safebump.adapters.dart;

import io.safebump.core.adapter.DependencySourceException;

import java.nio.file.Path;

/** Supplies Dart's resolved dependency graph as JSON for a project. */
@FunctionalInterface
interface DartDependencyExporter {

    String export(Path projectDirectory) throws DependencySourceException;
}
