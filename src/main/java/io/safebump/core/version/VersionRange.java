package io.safebump.core.version;

import java.util.Objects;
import java.util.Optional;

/** One continuous interval of semantic versions. */
public final class VersionRange {

    private final SemanticVersion lowerBound;
    private final boolean lowerInclusive;
    private final SemanticVersion upperBound;
    private final boolean upperInclusive;

    private VersionRange(
            SemanticVersion lowerBound,
            boolean lowerInclusive,
            SemanticVersion upperBound,
            boolean upperInclusive) {
        if (lowerBound != null && upperBound != null) {
            int comparison = lowerBound.compareTo(upperBound);
            if (comparison > 0 || (comparison == 0 && (!lowerInclusive || !upperInclusive))) {
                throw new IllegalArgumentException("Version range must contain at least one version");
            }
        }
        this.lowerBound = lowerBound;
        this.lowerInclusive = lowerInclusive;
        this.upperBound = upperBound;
        this.upperInclusive = upperInclusive;
    }

    public static VersionRange any() {
        return new VersionRange(null, false, null, false);
    }

    public static VersionRange exact(SemanticVersion version) {
        Objects.requireNonNull(version, "version");
        return new VersionRange(version, true, version, true);
    }

    public static VersionRange atLeast(SemanticVersion version, boolean inclusive) {
        return new VersionRange(Objects.requireNonNull(version, "version"), inclusive, null, false);
    }

    public static VersionRange atMost(SemanticVersion version, boolean inclusive) {
        return new VersionRange(null, false, Objects.requireNonNull(version, "version"), inclusive);
    }

    public static VersionRange between(
            SemanticVersion lowerBound,
            boolean lowerInclusive,
            SemanticVersion upperBound,
            boolean upperInclusive) {
        return new VersionRange(
                Objects.requireNonNull(lowerBound, "lowerBound"),
                lowerInclusive,
                Objects.requireNonNull(upperBound, "upperBound"),
                upperInclusive);
    }

    public Optional<SemanticVersion> lowerBound() {
        return Optional.ofNullable(lowerBound);
    }

    public boolean lowerInclusive() {
        return lowerInclusive;
    }

    public Optional<SemanticVersion> upperBound() {
        return Optional.ofNullable(upperBound);
    }

    public boolean upperInclusive() {
        return upperInclusive;
    }

    public boolean isAny() {
        return lowerBound == null && upperBound == null;
    }

    public boolean contains(SemanticVersion version) {
        Objects.requireNonNull(version, "version");
        if (lowerBound != null) {
            int lowerComparison = version.compareTo(lowerBound);
            if (lowerComparison < 0 || (lowerComparison == 0 && !lowerInclusive)) {
                return false;
            }
        }
        if (upperBound != null) {
            int upperComparison = version.compareTo(upperBound);
            if (upperComparison > 0 || (upperComparison == 0 && !upperInclusive)) {
                return false;
            }
        }
        return true;
    }

    public Optional<VersionRange> intersect(VersionRange other) {
        Objects.requireNonNull(other, "other");

        Bound lower = greaterLowerBound(this, other);
        Bound upper = lesserUpperBound(this, other);
        if (lower.version != null && upper.version != null) {
            int comparison = lower.version.compareTo(upper.version);
            if (comparison > 0 || (comparison == 0 && (!lower.inclusive || !upper.inclusive))) {
                return Optional.empty();
            }
        }
        return Optional.of(new VersionRange(
                lower.version, lower.inclusive, upper.version, upper.inclusive));
    }

    public boolean overlaps(VersionRange other) {
        return intersect(other).isPresent();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof VersionRange range)) {
            return false;
        }
        return lowerInclusive == range.lowerInclusive
                && upperInclusive == range.upperInclusive
                && Objects.equals(lowerBound, range.lowerBound)
                && Objects.equals(upperBound, range.upperBound);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lowerBound, lowerInclusive, upperBound, upperInclusive);
    }

    @Override
    public String toString() {
        if (isAny()) {
            return "any";
        }
        if (lowerBound != null
                && upperBound != null
                && lowerInclusive
                && upperInclusive
                && lowerBound.compareTo(upperBound) == 0) {
            return lowerBound.toString();
        }

        StringBuilder result = new StringBuilder();
        if (lowerBound != null) {
            result.append(lowerInclusive ? ">=" : ">").append(lowerBound);
        }
        if (upperBound != null) {
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(upperInclusive ? "<=" : "<").append(upperBound);
        }
        return result.toString();
    }

    private static Bound greaterLowerBound(VersionRange left, VersionRange right) {
        if (left.lowerBound == null) {
            return new Bound(right.lowerBound, right.lowerInclusive);
        }
        if (right.lowerBound == null) {
            return new Bound(left.lowerBound, left.lowerInclusive);
        }
        int comparison = left.lowerBound.compareTo(right.lowerBound);
        if (comparison > 0) {
            return new Bound(left.lowerBound, left.lowerInclusive);
        }
        if (comparison < 0) {
            return new Bound(right.lowerBound, right.lowerInclusive);
        }
        return new Bound(left.lowerBound, left.lowerInclusive && right.lowerInclusive);
    }

    private static Bound lesserUpperBound(VersionRange left, VersionRange right) {
        if (left.upperBound == null) {
            return new Bound(right.upperBound, right.upperInclusive);
        }
        if (right.upperBound == null) {
            return new Bound(left.upperBound, left.upperInclusive);
        }
        int comparison = left.upperBound.compareTo(right.upperBound);
        if (comparison < 0) {
            return new Bound(left.upperBound, left.upperInclusive);
        }
        if (comparison > 0) {
            return new Bound(right.upperBound, right.upperInclusive);
        }
        return new Bound(left.upperBound, left.upperInclusive && right.upperInclusive);
    }

    private record Bound(SemanticVersion version, boolean inclusive) {
    }
}
