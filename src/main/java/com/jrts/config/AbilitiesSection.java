package com.jrts.config;

import java.util.List;

/**
 * Abilities section parsed from TOML [abilities] block.
 */
public record AbilitiesSection(
        DeploySection deploy,
        DockingAbilitySection docking) {

    public record DeploySection(
            boolean enabled,
            float deployTime,
            List<DeployAction> onDeploy) {
    }

    public record DeployAction(
            String type,
            String weapon,
            String stat,
            float value) {
    }

    public record DockingAbilitySection(
            boolean enabled,
            String dockType,
            float unloadTime,
            int resourceCapacity) {
    }
}
