package com.jrts.unit;

import com.jrts.config.*;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UnitRegistryTest {

    private UnitRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new UnitRegistry();
    }

    @Test
    void registerAndFindById() {
        Unit unit = createTestUnit(1);
        registry.register(unit);
        assertTrue(registry.findById(1).isPresent());
        assertEquals(unit, registry.findById(1).get());
    }

    @Test
    void findBySpatialAfterRegister() {
        Unit unit = createTestUnit(1);
        registry.register(unit);
        assertTrue(registry.findBySpatial(unit.getSpatial()).isPresent());
    }

    @Test
    void findByEntityIdAfterRegister() {
        Unit unit = createTestUnit(1);
        registry.register(unit);
        assertTrue(registry.findByEntityId(unit.getSpatial()).isPresent());
    }

    @Test
    void unregisterRemovesFromBothMaps() {
        Unit unit = createTestUnit(1);
        registry.register(unit);
        registry.unregister(unit);
        assertFalse(registry.findById(1).isPresent());
        assertFalse(registry.findBySpatial(unit.getSpatial()).isPresent());
    }

    @Test
    void allUnitsReturnsAllRegistered() {
        Unit a = createTestUnit(1);
        Unit b = createTestUnit(2);
        registry.register(a);
        registry.register(b);

        List<Unit> all = registry.allUnits();
        assertEquals(2, all.size());
        assertTrue(all.contains(a));
        assertTrue(all.contains(b));
    }

    @Test
    void countReturnsCorrectSize() {
        assertEquals(0, registry.count());
        registry.register(createTestUnit(1));
        assertEquals(1, registry.count());
    }

    @Test
    void unitsWithFlagFiltersCorrectly() {
        Unit movable = createUnitWithFlags(1, UnitFlags.CAN_MOVE | UnitFlags.SELECTABLE);
        Unit structure = createUnitWithFlags(2, UnitFlags.IS_STRUCTURE);
        registry.register(movable);
        registry.register(structure);

        List<Unit> movableUnits = registry.unitsWithFlag(UnitFlags.CAN_MOVE);
        assertEquals(1, movableUnits.size());
        assertEquals(movable, movableUnits.get(0));
    }

    @Test
    void findByIdMissingReturnsEmpty() {
        assertTrue(registry.findById(999).isEmpty());
    }

    private static Unit createTestUnit(int id) {
        return createUnitWithFlags(id, UnitFlags.CAN_MOVE | UnitFlags.SELECTABLE);
    }

    private static Unit createUnitWithFlags(int id, int flags) {
        IdentitySection identity = new IdentitySection("Test" + id, "Test Unit",
                "AFV", "Republic", false, false, true, true, true, false,
                false, false, "unit");
        StatsSection stats = new StatsSection(100, "light", 5, 8f, 5f, 5f,
                100, 10, 1, -1, 10f);
        UnitConfig config = new UnitConfig(identity, stats, null, 0,
                new VeterancySection(false, null, null),
                new CombatSection("", "", "", "", -1, 0f, false, 64, false, false, false),
                new AbilitiesSection(
                        new AbilitiesSection.DeploySection(false, 0f, null),
                        new AbilitiesSection.DockingAbilitySection(false, "", 0f, 0)),
                new PassengersSection(0),
                new AudioSection(null, null, null, null, null, null, ""),
                new MovementSection("tracks", false, false, false),
                new TurretsSection(false, false, null, 0f, null, 0f),
                new BuildingInteractionsSection("", "", false, false, false),
                new SpecialFlagsSection(false));

        Node spatial = new Node("TestSpatial" + id);
        return new Unit(id, config, spatial, flags);
    }
}
