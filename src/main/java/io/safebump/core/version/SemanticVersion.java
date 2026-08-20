package io.safebump.core.version;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A semantic version using Dart pub's ordering, including build suffixes. */
public final class SemanticVersion implements Comparable<SemanticVersion> {

    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)"
                    + "(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?"
                    + "(?:\\+([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?$");

    private final BigInteger major;
    private final BigInteger minor;
    private final BigInteger patch;
    private final List<String> preRelease;
    private final List<String> build;

    private SemanticVersion(
            BigInteger major,
            BigInteger minor,
            BigInteger patch,
            List<String> preRelease,
            List<String> build) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preRelease = List.copyOf(preRelease);
        this.build = List.copyOf(build);
    }

    public static SemanticVersion parse(String value) {
        Objects.requireNonNull(value, "value");
        String trimmedValue = value.trim();
        Matcher matcher = VERSION_PATTERN.matcher(trimmedValue);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Invalid semantic version: " + trimmedValue);
        }

        List<String> preRelease = identifiers(matcher.group(4));
        for (String identifier : preRelease) {
            if (isNumeric(identifier)
                    && identifier.length() > 1
                    && identifier.startsWith("0")) {
                throw new IllegalArgumentException(
                        "Numeric pre-release identifiers must not contain leading zeroes: "
                                + trimmedValue);
            }
        }

        return new SemanticVersion(
                new BigInteger(matcher.group(1)),
                new BigInteger(matcher.group(2)),
                new BigInteger(matcher.group(3)),
                preRelease,
                identifiers(matcher.group(5)));
    }

    public BigInteger major() {
        return major;
    }

    public BigInteger minor() {
        return minor;
    }

    public BigInteger patch() {
        return patch;
    }

    public List<String> preRelease() {
        return preRelease;
    }

    public List<String> build() {
        return build;
    }

    public boolean isPreRelease() {
        return !preRelease.isEmpty();
    }

    @Override
    public int compareTo(SemanticVersion other) {
        Objects.requireNonNull(other, "other");

        int comparison = major.compareTo(other.major);
        if (comparison == 0) {
            comparison = minor.compareTo(other.minor);
        }
        if (comparison == 0) {
            comparison = patch.compareTo(other.patch);
        }
        if (comparison != 0) {
            return comparison;
        }
        comparison = comparePreRelease(preRelease, other.preRelease);
        if (comparison != 0) {
            return comparison;
        }
        return compareBuild(build, other.build);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SemanticVersion version)) {
            return false;
        }
        return major.equals(version.major)
                && minor.equals(version.minor)
                && patch.equals(version.patch)
                && preRelease.equals(version.preRelease)
                && build.equals(version.build);
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch, preRelease, build);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder()
                .append(major).append('.')
                .append(minor).append('.')
                .append(patch);
        if (!preRelease.isEmpty()) {
            result.append('-').append(String.join(".", preRelease));
        }
        if (!build.isEmpty()) {
            result.append('+').append(String.join(".", build));
        }
        return result.toString();
    }

    private static List<String> identifiers(String value) {
        if (value == null) {
            return List.of();
        }
        return List.of(value.split("\\."));
    }

    private static int comparePreRelease(List<String> left, List<String> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return left.isEmpty() ? (right.isEmpty() ? 0 : 1) : -1;
        }

        return compareIdentifiers(left, right);
    }

    private static int compareBuild(List<String> left, List<String> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return left.isEmpty() ? (right.isEmpty() ? 0 : -1) : 1;
        }
        return compareIdentifiers(left, right);
    }

    private static int compareIdentifiers(List<String> left, List<String> right) {
        int sharedLength = Math.min(left.size(), right.size());
        for (int index = 0; index < sharedLength; index++) {
            String leftIdentifier = left.get(index);
            String rightIdentifier = right.get(index);
            int comparison = compareIdentifier(leftIdentifier, rightIdentifier);
            if (comparison != 0) {
                return comparison;
            }
        }
        return Integer.compare(left.size(), right.size());
    }

    private static int compareIdentifier(String left, String right) {
        boolean leftNumeric = isNumeric(left);
        boolean rightNumeric = isNumeric(right);
        if (leftNumeric && rightNumeric) {
            return new BigInteger(left).compareTo(new BigInteger(right));
        }
        if (leftNumeric != rightNumeric) {
            return leftNumeric ? -1 : 1;
        }
        return left.compareTo(right);
    }

    private static boolean isNumeric(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (!Character.isDigit(value.charAt(index))) {
                return false;
            }
        }
        return true;
    }
}
