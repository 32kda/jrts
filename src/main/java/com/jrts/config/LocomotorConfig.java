package com.jrts.config;

/**
 * Locomotor configuration parsed from TOML locomotor definitions.
 * Stage 1: basic profile used by MovementController.
 */
public record LocomotorConfig(
        String type,
        float maxSpeed,
        float acceleration,
        float deceleration,
        float maxTurnRate,
        float closeEnoughDist,
        float preferredHeight) {

    /**
     * Default locomotor for Stage 1 — derived from unit stats.
     */
    public static LocomotorConfig fromUnitConfig(UnitConfig config) {
        return new LocomotorConfig(
                config.movement() != null ? config.movement().locomotor() : "tracks",
                config.stats().speed(),
                10f,
                15f,
                config.stats().rot(),
                0.5f,
                0f);
    }
}
