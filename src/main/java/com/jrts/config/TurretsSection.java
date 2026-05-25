package com.jrts.config;

import java.util.List;

/**
 * Turrets section parsed from TOML [turrets] block.
 */
public record TurretsSection(
        boolean turret,
        boolean turretSpins,
        List<Float> turretRotationYaw,
        float turretRotationSpeed,
        List<Float> barrelElevationPitch,
        float barrelSpeed) {
}
