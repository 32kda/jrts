package com.jrts.config;

/**
 * Building interactions section parsed from TOML [building_interactions] block.
 */
public record BuildingInteractionsSection(
        String dock,
        String freeUnit,
        boolean unitReload,
        boolean unitRepair,
        boolean dockUnload) {
}
