package io.safebump.core.model;

/** Describes how a resolved package relates to the analysed project. */
public enum DependencyKind {
    ROOT,
    DIRECT,
    DEV,
    TRANSITIVE
}
