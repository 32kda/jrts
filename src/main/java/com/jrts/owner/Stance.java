package com.jrts.owner;

/**
 * Diplomatic relationship between two owners (players), from the perspective of
 * a reference owner toward another.
 */
public enum Stance {
    /** Same owner — fully controllable and never targeted by default. */
    FRIEND,
    /** Neutral (owner 0 / civilians) — not controllable, not auto-targeted. */
    NEUTRAL,
    /** Hostile — auto-targetable and attackable. */
    ENEMY
}
