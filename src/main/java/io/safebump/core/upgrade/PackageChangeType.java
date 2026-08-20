package io.safebump.core.upgrade;

/** How one named package changed between two resolved dependency states. */
public enum PackageChangeType {
    ADDED,
    REMOVED,
    UPGRADED,
    DOWNGRADED,
    UNCHANGED
}
