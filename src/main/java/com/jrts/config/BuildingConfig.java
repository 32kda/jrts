package com.jrts.config;

/**
 * Full building definition parsed from building_example.toml.
 */
public record BuildingConfig(
        BuildingIdentitySection identity,
        BuildingStatsSection stats,
        ProductionSection production,
        WeaponConfig defenseWeapon,
        BuildingDockingSection docking) {
}
