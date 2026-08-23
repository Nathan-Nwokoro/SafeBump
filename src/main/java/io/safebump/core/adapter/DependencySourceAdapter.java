package io.safebump.core.adapter;

import io.safebump.core.model.DependencySnapshot;

import java.nio.file.Path;

/** Converts ecosystem-specific dependency data into SafeBump's core model. */
public interface DependencySourceAdapter {

    DependencySnapshot load(Path source) throws DependencySourceException;
}
