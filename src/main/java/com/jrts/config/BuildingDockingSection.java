package com.jrts.config;

/**
 * Docking section for buildings.
 */
public record BuildingDockingSection(
        boolean enabled,
        String dockType,
        int unloadSpeed,
        boolean harvesterQueue) {
}
