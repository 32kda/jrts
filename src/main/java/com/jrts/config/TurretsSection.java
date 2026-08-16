package com.jrts.config;

import java.util.List;

/**
 * Turrets section parsed from TOML [turrets] block.
 *
 * <p>Angles are stored in degrees (as authored in TOML); conversion to
 * radians happens later in {@code com.jrts.turret.TurretConfig}.</p>
 */
public record TurretsSection(
        boolean turret,
        boolean turretSpins,
        List<Float> turretRotationYaw,
        float turretRotationSpeed,
        List<Float> barrelElevationPitch,
        float barrelSpeed,
        boolean allowsPitch,
        float naturalTurretAngle,
        float naturalTurretPitch,
        float firePitch,
        float minIdleScanAngle,
        float maxIdleScanAngle,
        float idleScanInterval,
        float recenterTime) {
}
