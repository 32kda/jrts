package com.jrts.config;

/**
 * Full weapon definition parsed from weapon_example.toml.
 */
public record WeaponConfig(
        String name,
        String type,
        String projectileScene,
        float damage,
        String damageType,
        boolean areaDamage,
        float explosionRadius,
        float range,
        float cooldown,
        float minRange,
        float ballisticArc,
        float accuracy,
        float scatterRadius,
        float particleSpeed,
        boolean shootingCorrection,
        boolean homing) {
}
