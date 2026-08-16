package com.jrts.unit;

import com.jrts.config.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class UnitFlagsTest {

    @Test
    void fromConfig_tankSetsCanMoveSelectableHasTurret() {
        UnitConfig tankConfig = createTankConfig();
        int flags = UnitFlags.fromUnitConfig(tankConfig);
        assertTrue((flags & UnitFlags.CAN_MOVE) != 0);
        assertTrue((flags & UnitFlags.SELECTABLE) != 0);
        assertTrue((flags & UnitFlags.HAS_TURRET) != 0);
        assertFalse((flags & UnitFlags.IS_STRUCTURE) != 0);
    }

    @Test
    void fromConfig_buildingSetsStructure() {
        UnitConfig buildingConfig = createBuildingConfig();
        int flags = UnitFlags.fromUnitConfig(buildingConfig);
        assertTrue((flags & UnitFlags.IS_STRUCTURE) != 0);
        assertFalse((flags & UnitFlags.CAN_MOVE) != 0);
    }

    @Test
    void fromConfig_nonSelectableUnitHasNoSelectFlags() {
        UnitConfig config = createConfig(false, false, false);
        int flags = UnitFlags.fromUnitConfig(config);
        assertFalse((flags & UnitFlags.SELECTABLE) != 0);
        assertFalse((flags & UnitFlags.CAN_BE_SELECTED) != 0);
    }

    private static UnitConfig createTankConfig() {
        return createConfig(true, true, true);
    }

    private static UnitConfig createBuildingConfig() {
        return createConfig(false, false, true);
    }

    private static UnitConfig createConfig(boolean canMove, boolean selectable,
                                           boolean hasTurret) {
        IdentitySection identity = new IdentitySection("Test", "Test Unit", "AFV",
                "Republic", false, false, true, selectable, true, false,
                false, false, canMove ? "unit" : "building");
        StatsSection stats = new StatsSection(100, "light", 5, 8f, 5f, 5f,
                100, 10, 1, -1, 10f);
        TurretsSection turrets = hasTurret
                ? new TurretsSection(true, false, Arrays.asList(-180f, 180f), 5f,
                Arrays.asList(-5f, 45f), 4f, true, 0f, 0f, 0f, -15f, 15f, 5f, 2f)
                : new TurretsSection(false, false, null, 0f, null, 0f,
                false, 0f, 0f, 0f, 0f, 0f, 5f, 2f);
        MovementSection movement = new MovementSection("tracks", false, true, false);
        CombatSection combat = new CombatSection("", "", "", "", -1, 0f, false, 64, false, false, false);

        return new UnitConfig(identity, stats, null, 0,
                new VeterancySection(false, null, null),
                combat,
                new AbilitiesSection(
                        new AbilitiesSection.DeploySection(false, 0f, null),
                        new AbilitiesSection.DockingAbilitySection(false, "", 0f, 0)),
                new PassengersSection(0),
                new AudioSection(null, null, null, null, null, null, ""),
                movement, turrets,
                new BuildingInteractionsSection("", "", false, false, false),
                new SpecialFlagsSection(false));
    }
}
