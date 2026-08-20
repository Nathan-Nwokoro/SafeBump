package io.safebump.core.adapter;

/** Indicates that dependency source data could not be read or validated. */
public final class DependencySourceException extends Exception {

    public DependencySourceException(String message) {
        super(message);
    }

    public DependencySourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
