package com.jrts.config;

/**
 * Full building definition parsed from building_example.toml.
 *
 * <p>{@code turrets} is present for defensive structures (guard towers); a
 * {@code null} value means the building has no turret. {@code defenseWeapon}
 * is the weapon fired by the turret.
 */
public record BuildingConfig(
        BuildingIdentitySection identity,
        BuildingStatsSection stats,
        ProductionSection production,
        WeaponConfig defenseWeapon,
        BuildingDockingSection docking,
        TurretsSection turrets) {

    /**
     * @return true if this building mounts a defensive turret
     */
    public boolean hasTurret() {
        return turrets != null && turrets.turret();
    }
}
