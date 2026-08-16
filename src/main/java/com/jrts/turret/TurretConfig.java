package com.jrts.turret;

import com.jrts.config.TurretsSection;
import com.jme3.math.FastMath;

import java.util.List;

/**
 * Resolved turret parameters for the turret state machine.
 *
 * <p>All angles and rates are expressed in radians / radians-per-second,
 * converted from the degree-based values authored in the {@code [turrets]}
 * TOML section. This is the domain model consumed by
 * {@link TurretStateMachine} and {@link TurretControl}; it never leaks raw
 * TOML lists or degree units into the game loop.</p>
 *
 * <p>Derived from the spec's TurretAI data table (section 7.2) and the
 * {@code TurretAIData} reference, limited to the fields the state machine
 * actually needs.</p>
 */
public record TurretConfig(
        float turnRate,
        float pitchRate,
        boolean allowsPitch,
        float naturalTurretAngle,
        float naturalTurretPitch,
        float firePitch,
        float minPitch,
        float maxPitch,
        float minIdleScanAngle,
        float maxIdleScanAngle,
        float idleScanInterval,
        float recenterTime) {

    /** Default time between idle scans (seconds) when not configured. */
    private static final float DEFAULT_IDLE_SCAN_INTERVAL = 5.0f;
    /** Default hold time (seconds) before a lost target triggers recentering. */
    private static final float DEFAULT_RECENTER_TIME = 2.0f;

    /**
     * Builds a resolved config from the raw parsed TOML section, applying
     * degree-to-radian conversion and defaults for optional values.
     *
     * @param section parsed {@code [turrets]} block, or {@code null} for a
     *                disabled turret
     * @return resolved config; {@link #allowsPitch()} is always false when
     *         {@code section} is null
     */
    public static TurretConfig from(TurretsSection section) {
        if (section == null) {
            return new TurretConfig(0f, 0f, false, 0f, 0f, 0f, 0f, 0f, 0f, 0f,
                    DEFAULT_IDLE_SCAN_INTERVAL, DEFAULT_RECENTER_TIME);
        }
        return new TurretConfig(
                toRadians(section.turretRotationSpeed()),
                toRadians(section.barrelSpeed()),
                section.allowsPitch(),
                toRadians(section.naturalTurretAngle()),
                toRadians(section.naturalTurretPitch()),
                toRadians(section.firePitch()),
                toRadians(minOf(section.barrelElevationPitch(), 0f)),
                toRadians(maxOf(section.barrelElevationPitch(), 0f)),
                toRadians(section.minIdleScanAngle()),
                toRadians(section.maxIdleScanAngle()),
                section.idleScanInterval(),
                section.recenterTime());
    }

    private static float toRadians(float degrees) {
        return degrees * FastMath.DEG_TO_RAD;
    }

    private static float minOf(List<Float> values, float fallback) {
        if (values == null || values.isEmpty()) {
            return fallback;
        }
        float min = Float.MAX_VALUE;
        for (Float value : values) {
            min = Math.min(min, value);
        }
        return min;
    }

    private static float maxOf(List<Float> values, float fallback) {
        if (values == null || values.isEmpty()) {
            return fallback;
        }
        float max = -Float.MAX_VALUE;
        for (Float value : values) {
            max = Math.max(max, value);
        }
        return max;
    }
}
