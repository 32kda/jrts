package com.jrts.config;

/**
 * Stats section parsed from TOML [stats] block.
 * Note: buildings use "health" instead of "strength".
 */
public record StatsSection(
        int strength,
        String armor,
        int sight,
        float guardRange,
        float speed,
        float rot,
        int cost,
        int points,
        int techLevel,
        int buildLimit,
        float buildTime) {
}
