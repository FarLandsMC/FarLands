package net.farlands.sanctuary.util;

/**
 * This is a realively simple class that aids in checking range bounds and offers a useful parser for "Rust-style" range bounds.
 *
 * @param <T> The type which is compared.  This can be anything that implements {@link Comparable<T>}
 */
public class Range<T extends Comparable<T>> {

    /**
     * Includes n for all n
     */
    public static final Range<?> UNBOUND = new Range<>(null, null, true, true);

    /**
     * Same as {@link Range#UNBOUND}, except for Integers
     */
    public static final Range<Integer> UNBOUND_INT = new Range<>(null, null, true, true, Integer.class);

    private final T        low;
    private final T        high;
    private final boolean  inclLow;
    private final boolean  inclHigh;
    private final Class<T> clazz; // This is only for toString and it's very stupid that this is the _preferred_ way to have it

    /**
     * Constructor for the Range
     *
     * @param low      The low bound of the range
     * @param high     The high bound of the range
     * @param inclLow  Whether the low bound is inclusive
     * @param inclHigh Whether the high bound is inclusive
     * @param clazz    The class to use for checking {@link T} (Currently only used in toString to check for Integer)
     */
    private Range(T low, T high, boolean inclLow, boolean inclHigh, Class<T> clazz) {
        this.low = low;
        this.high = high;
        this.inclLow = inclLow;
        this.inclHigh = inclHigh;
        this.clazz = clazz;
    }

    /**
     * Constructor for the Range
     *
     * @param low      The low bound of the range
     * @param high     The high bound of the range
     * @param inclLow  Whether the low bound is inclusive
     * @param inclHigh Whether the high bound is inclusive
     */
    public Range(T low, T high, boolean inclLow, boolean inclHigh) {
        this(low, high, inclLow, inclHigh, null);
    }

    /**
     * Checks if the item is contained within the range
     *
     * @param n The item to check
     * @return If the item was within the range
     */
    public boolean contains(T n) {
        int c;
        return n != null
        && (this.low  == null || (c = n.compareTo(this.low )) > 0 || (this.inclLow  && c == 0))
        && (this.high == null || (c = n.compareTo(this.high)) < 0 || (this.inclHigh && c == 0));
    }

    /**
     * @return the lower bound of this range
     */
    public T low() {
        return this.low;
    }

    /**
     * @return the higher bound of this range
     */
    public T high() {
        return this.high;
    }

    /**
     * @return if this range includes the lower bound
     */
    public boolean includeLow() {
        return this.inclLow;
    }

    /**
     * @return if this range includes the higher bound
     */
    public boolean includeHigh() {
        return this.inclHigh;
    }

    /**
     * Parse a "Rust-style" range string into a {@link Range<Integer>}, like the following:
     * <ul>
     *     <li>{@code a..b}</li>
     *     <li>{@code a..=b}</li>
     *     <li>{@code a..}</li>
     *     <li>{@code ..b}</li>
     *     <li>{@code ..=b}</li>
     *     <li>{@code ..}</li>
     * </ul>
     * <p>
     * Range details: <ul>
     *     <li>The lower bound, if provided, is included in the comparison</li>
     *     <li>The upper bound, by default is not included, but providing {@code =} includes it</li>
     * </ul>
     *
     * @param rangeStr The string to be parsed into the Range
     * @return The range that has been parsed
     * @throws IllegalArgumentException When rangeStr is not a valid range string
     */
    public static Range<Integer> from(String rangeStr) throws IllegalArgumentException {
        Integer low;
        Integer high;
        boolean inclHigh;

        if (!rangeStr.contains("..")) {
            throw new IllegalArgumentException("Invalid range string");
        }

        try {
            String[] parts = rangeStr.split("\\.\\.");
            if (parts.length == 0) { // ".."
                return Range.UNBOUND_INT;
            }

            if (parts[0].isEmpty()) {
                low = null;
            } else {
                low = Integer.parseInt(parts[0]);
            }

            if (parts.length == 1) {
                high = null;
                inclHigh = false;
            } else {
                if (parts[1].startsWith("=")) {
                    inclHigh = true;
                    parts[1] = parts[1].substring(1);
                } else {
                    inclHigh = false;
                }
                high = Integer.parseInt(parts[1]);
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid range string", ex);
        }

        return new Range<>(low, high, true, inclHigh, Integer.class);
    }

    /**
     * Check if {@link T} matches the provided class
     */
    private <U> boolean isT(Class<U> clazz) {
        return this.clazz != null && this.clazz.equals(clazz);
    }

    /**
     * Convert to a string
     * <p>
     * If {@link T} is of type {@link Integer} and {@code Integer.class} was passed to the constructor, it will show using the "rust-style" notation, otherwise it will use interval notation.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.isT(Integer.class)) { // If it's an int, use rust-style, i.e. n..m or n..=m
            if (this.low != null) {
                int low = (Integer) this.low;
                sb.append(this.inclLow ? low : low + 1);
            }
            sb.append("..");
            if (this.high != null) {
                if (this.inclHigh) {
                    sb.append('=');
                }
                sb.append(this.high);
            }
        } else { // Otherwise, use interval notation, i.e. [n, m) or [n, m]
            sb.append(this.inclLow ? '[' : '(');
            sb.append(this.low);
            sb.append(", ");
            sb.append(this.high);
            sb.append(this.inclHigh ? ']' : ')');
        }
        return sb.toString();
    }
}
