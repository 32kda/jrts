package com.jrts.turret;

/**
 * Turret configuration from [turrets] TOML section.
 */
public record TurretConfig(
        boolean turret,
        boolean turretSpins,
        float turretRotationSpeed,
        float barrelSpeed,
        float minYawDeg,
        float maxYawDeg,
        float minPitchDeg,
        float maxPitchDeg) {
}
