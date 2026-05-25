package com.jrts.config;

import java.util.List;

/**
 * Combat section parsed from TOML [combat] block.
 */
public record CombatSection(
        String primaryWeapon,
        String secondaryWeapon,
        String elitePrimary,
        String eliteSecondary,
        int ammo,
        float reloadTime,
        boolean manualReload,
        int fireAngle,
        boolean targetLaser,
        boolean deployToFire,
        boolean noMovingFire) {
}
