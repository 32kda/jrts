package com.jrts.config;

/**
 * Building stats section (health instead of strength, power fields).
 */
public record BuildingStatsSection(
        int health,
        String armorType,
        int sightRange,
        int powerRequired,
        int powerProvided) {
}
