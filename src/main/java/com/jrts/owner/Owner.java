package com.jrts.owner;

/**
 * Ownership identifiers for runtime units and buildings.
 *
 * <p>An owner is a byte-sized player index:
 * <ul>
 *   <li>{@code 0} — {@link #NEUTRAL}: uncontrolled / civilian (map-placed decoration buildings)</li>
 *   <li>{@code 1} — {@link #PLAYER}: the human player</li>
 *   <li>{@code 2..255} — additional players (AI opponents or allies)</li>
 * </ul>
 *
 * The relationship between two owners is a {@link Stance}, resolved by
 * {@link StanceResolver}. This is deliberately just a number, so a match can
 * host more than two players without a fixed enum.
 */
public final class Owner {

    public static final int NEUTRAL = 0;
    public static final int PLAYER = 1;

    public static final int MAX = 255;

    private Owner() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * @return true if the value fits the owner byte range (0..255)
     */
    public static boolean isValid(int owner) {
        return owner >= 0 && owner <= MAX;
    }
}
