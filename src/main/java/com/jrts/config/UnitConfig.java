package com.jrts.config;

import java.util.List;

/**
 * Full unit definition parsed from heavy_tank_example.toml (and similar).
 * Each [section] in TOML maps to a separate composed object.
 * No inheritance — a building and a tank both use UnitConfig but
 * differ in which sub-objects are present (flag: isStructure).
 */
public record UnitConfig(
        IdentitySection identity,
        StatsSection stats,
        List<String> prerequisites,
        int buildingsRequired,
        VeterancySection veterancy,
        CombatSection combat,
        AbilitiesSection abilities,
        PassengersSection passengers,
        AudioSection audio,
        MovementSection movement,
        TurretsSection turrets,
        BuildingInteractionsSection buildingInteractions,
        SpecialFlagsSection specialFlags) {

    /**
     * @return true if this config describes a structure/building
     */
    public boolean isStructure() {
        return "building".equalsIgnoreCase(identity.type());
    }
}
