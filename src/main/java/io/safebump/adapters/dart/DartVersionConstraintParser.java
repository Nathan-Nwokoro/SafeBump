package io.safebump.adapters.dart;

import io.safebump.core.version.SemanticVersion;
import io.safebump.core.version.VersionRange;

import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses the continuous version-constraint forms supported by Dart pub. */
public final class DartVersionConstraintParser {

    private static final Pattern COMPARATOR = Pattern.compile("^(>=|<=|>|<)?(.+)$");

    public VersionRange parse(String value) {
        Objects.requireNonNull(value, "value");
        String constraint = value.trim();
        if (constraint.isEmpty()) {
            throw new IllegalArgumentException("Version constraint must not be blank");
        }
        if (constraint.equals("any")) {
            return VersionRange.any();
        }
        if (constraint.startsWith("^")) {
            if (constraint.indexOf(' ', 1) >= 0) {
                throw invalid(value);
            }
            return caret(SemanticVersion.parse(constraint.substring(1)));
        }

        VersionRange result = VersionRange.any();
        String[] parts = constraint.split("\\s+");
        for (String part : parts) {
            Matcher matcher = COMPARATOR.matcher(part);
            if (!matcher.matches()) {
                throw invalid(value);
            }
            SemanticVersion version = SemanticVersion.parse(matcher.group(2));
            VersionRange partRange = switch (matcher.group(1) == null
                    ? "=" : matcher.group(1)) {
                case ">=" -> VersionRange.atLeast(version, true);
                case ">" -> VersionRange.atLeast(version, false);
                case "<=" -> VersionRange.atMost(version, true);
                case "<" -> VersionRange.atMost(version, false);
                case "=" -> VersionRange.exact(version);
                default -> throw invalid(value);
            };
            Optional<VersionRange> intersection = result.intersect(partRange);
            if (intersection.isEmpty()) {
                throw new IllegalArgumentException(
                        "Version constraint contains incompatible bounds: " + value);
            }
            result = intersection.orElseThrow();
        }
        return result;
    }

    private static VersionRange caret(SemanticVersion lowerBound) {
        SemanticVersion upperBound;
        if (lowerBound.major().signum() > 0) {
            upperBound = version(lowerBound.major().add(BigInteger.ONE), BigInteger.ZERO,
                    BigInteger.ZERO);
        } else {
            upperBound = version(BigInteger.ZERO, lowerBound.minor().add(BigInteger.ONE),
                    BigInteger.ZERO);
        }
        return VersionRange.between(lowerBound, true, upperBound, false);
    }

    private static SemanticVersion version(
            BigInteger major,
            BigInteger minor,
            BigInteger patch) {
        return SemanticVersion.parse(major + "." + minor + "." + patch);
    }

    private static IllegalArgumentException invalid(String value) {
        return new IllegalArgumentException("Unsupported Dart version constraint: " + value);
    }
}
