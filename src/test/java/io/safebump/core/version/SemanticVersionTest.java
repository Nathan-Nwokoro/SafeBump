package io.safebump.core.version;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticVersionTest {

    @Test
    void parsesEverySemanticVersionComponent() {
        SemanticVersion version = SemanticVersion.parse("12.34.56-alpha.1+linux.arm64");

        assertEquals("12", version.major().toString());
        assertEquals("34", version.minor().toString());
        assertEquals("56", version.patch().toString());
        assertEquals(List.of("alpha", "1"), version.preRelease());
        assertEquals(List.of("linux", "arm64"), version.build());
        assertEquals("12.34.56-alpha.1+linux.arm64", version.toString());
    }

    @Test
    void followsSemanticVersionPrecedence() {
        List<SemanticVersion> ordered = List.of(
                "1.0.0-alpha",
                "1.0.0-alpha.1",
                "1.0.0-alpha.beta",
                "1.0.0-beta",
                "1.0.0-beta.2",
                "1.0.0-beta.11",
                "1.0.0-rc.1",
                "1.0.0").stream().map(SemanticVersion::parse).toList();

        for (int index = 1; index < ordered.size(); index++) {
            assertTrue(ordered.get(index - 1).compareTo(ordered.get(index)) < 0);
        }
    }

    @Test
    void followsDartPubBuildMetadataOrdering() {
        SemanticVersion first = SemanticVersion.parse("1.2.3+build.1");
        SemanticVersion second = SemanticVersion.parse("1.2.3+build.2");

        assertTrue(first.compareTo(second) < 0);
        assertTrue(SemanticVersion.parse("1.2.3").compareTo(first) < 0);
    }

    @Test
    void rejectsInvalidVersions() {
        assertThrows(IllegalArgumentException.class, () -> SemanticVersion.parse("1.2"));
        assertThrows(IllegalArgumentException.class, () -> SemanticVersion.parse("01.2.3"));
        assertThrows(
                IllegalArgumentException.class,
                () -> SemanticVersion.parse("1.2.3-alpha.01"));
        assertThrows(IllegalArgumentException.class, () -> SemanticVersion.parse("1.2.3+"));
    }
}
