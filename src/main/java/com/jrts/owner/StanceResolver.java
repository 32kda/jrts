package com.jrts.owner;

/**
 * Resolves the {@link Stance} between two {@link Owner} ids.
 *
 * <p>Default rules:
 * <ul>
 *   <li>target owner {@code 0} (neutral) → {@link Stance#NEUTRAL}</li>
 *   <li>same owner → {@link Stance#FRIEND}</li>
 *   <li>any other non-zero owner → {@link Stance#ENEMY}</li>
 * </ul>
 *
 * A full diplomacy table (configurable ally/enemy overrides per player) can
 * replace this default later without touching call sites.
 */
public final class StanceResolver {

    private StanceResolver() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * @param self  the reference owner id
     * @param other the owner id being evaluated
     */
    public static Stance stanceOf(int self, int other) {
        if (other == Owner.NEUTRAL) {
            return Stance.NEUTRAL;
        }
        if (other == self) {
            return Stance.FRIEND;
        }
        return Stance.ENEMY;
    }

    /**
     * @return true if {@code other} is hostile to {@code self}
     */
    public static boolean isHostile(int self, int other) {
        return stanceOf(self, other) == Stance.ENEMY;
    }

    /**
     * @return true if {@code other} is owned/controllable by {@code self}
     */
    public static boolean isFriendly(int self, int other) {
        return stanceOf(self, other) == Stance.FRIEND;
    }
}
