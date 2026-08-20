package io.safebump.adapters.dart;

import io.safebump.core.version.SemanticVersion;
import io.safebump.core.version.VersionRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DartVersionConstraintParserTest {

    private final DartVersionConstraintParser parser = new DartVersionConstraintParser();

    @Test
    void parsesAnyAndExactVersions() {
        assertTrue(parser.parse("any").isAny());
        assertEquals(VersionRange.exact(version("1.2.3")), parser.parse("1.2.3"));
    }

    @Test
    void parsesComparatorRanges() {
        VersionRange range = parser.parse(">=1.2.3 <2.0.0");

        assertTrue(range.contains(version("1.2.3")));
        assertTrue(range.contains(version("1.9.9")));
        assertFalse(range.contains(version("2.0.0")));
    }

    @Test
    void expandsStableCaretConstraint() {
        assertEquals(
                VersionRange.between(version("1.2.3"), true, version("2.0.0"), false),
                parser.parse("^1.2.3"));
    }

    @Test
    void expandsPreOneCaretConstraintsAtFirstNonZeroComponent() {
        assertEquals(
                VersionRange.between(version("0.2.3"), true, version("0.3.0"), false),
                parser.parse("^0.2.3"));
        assertEquals(
                VersionRange.between(version("0.0.3"), true, version("0.1.0"), false),
                parser.parse("^0.0.3"));
    }

    @Test
    void supportsInclusiveUpperBounds() {
        VersionRange range = parser.parse(">1.0.0 <=2.0.0");

        assertFalse(range.contains(version("1.0.0")));
        assertTrue(range.contains(version("2.0.0")));
    }

    @Test
    void rejectsUnsupportedAndContradictoryConstraints() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("^1.2.3 <2.0.0"));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(">2.0.0 <1.0.0"));
        assertThrows(IllegalArgumentException.class, () -> parser.parse("1.2"));
    }

    private static SemanticVersion version(String value) {
        return SemanticVersion.parse(value);
    }
}
