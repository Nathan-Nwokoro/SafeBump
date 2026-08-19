package io.safebump.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PackageVersionTest {

    @Test
    void trimsAndFormatsPackageIdentity() {
        PackageVersion packageVersion = new PackageVersion(" firebase_core ", " 3.8.1 ");

        assertEquals("firebase_core", packageVersion.name());
        assertEquals("3.8.1", packageVersion.version());
        assertEquals("firebase_core@3.8.1", packageVersion.toString());
    }

    @Test
    void rejectsMissingPackageIdentityFields() {
        assertThrows(NullPointerException.class, () -> new PackageVersion(null, "1.0.0"));
        assertThrows(NullPointerException.class, () -> new PackageVersion("firebase_core", null));
        assertThrows(IllegalArgumentException.class, () -> new PackageVersion("  ", "1.0.0"));
        assertThrows(IllegalArgumentException.class, () -> new PackageVersion("firebase_core", "  "));
    }
}
