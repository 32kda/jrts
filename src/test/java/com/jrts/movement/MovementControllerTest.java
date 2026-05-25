package com.jrts.movement;

import com.jrts.config.*;
import com.jrts.scene.FlatTerrainHeightProvider;
import com.jrts.scene.TerrainHeightProvider;
import com.jrts.unit.Unit;
import com.jrts.unit.UnitFlags;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MovementControllerTest {

    private TerrainHeightProvider terrain;
    private MovementController controller;

    @BeforeEach
    void setUp() {
        terrain = new FlatTerrainHeightProvider(0f, 100f);
        controller = new MovementController(terrain);
    }

    @Test
    void unitMovesTowardTargetEachFrame() {
        Unit unit = createUnitAt(0, 0, 0, 0);
        unit.setWaypoints(Collections.singletonList(new Vector3f(0, 0, 10)));

        boolean moving = controller.update(unit, 1.0f / 60f);
        assertTrue(moving);
        assertTrue(unit.getPosition().z > 0);
    }

    @Test
    void unitRotatesBeforeMoving() {
        Unit unit = createUnitAt(0, 0, 0, 0);
        unit.setWaypoints(Collections.singletonList(new Vector3f(10, 0, 0)));

        controller.update(unit, 1.0f / 60f);
        assertTrue(Math.abs(unit.getBodyYaw()) > 0.001f);
        assertTrue(unit.getPosition().x < 0.1f);
    }

    @Test
    void unitStopsAtCloseEnoughDistance() {
        Unit unit = createUnitAt(0, 0, 0, 0);
        unit.setWaypoints(Collections.singletonList(new Vector3f(0, 0, 0.3f)));

        boolean moving = controller.update(unit, 1.0f / 60f);
        assertFalse(moving);
        assertTrue(unit.getWaypoints().isEmpty());
    }

    @Test
    void movesThroughMultipleWaypoints() {
        Unit unit = createUnitAt(0, 0, 0, 0);
        unit.setWaypoints(Arrays.asList(
                new Vector3f(0, 0, 0.3f),
                new Vector3f(0, 0, 10f)
        ));

        controller.update(unit, 1.0f / 60f);
        assertEquals(1, unit.getWaypoints().size());
        assertEquals(new Vector3f(0, 0, 10f), unit.getWaypoints().get(0));
    }

    @Test
    void nullWaypointsReturnsFalse() {
        Unit unit = createUnitAt(0, 0, 0, 0);
        assertFalse(controller.update(unit, 1.0f / 60f));
    }

    @Test
    void emptyWaypointsReturnsFalse() {
        Unit unit = createUnitAt(0, 0, 0, 0);
        unit.setWaypoints(Collections.emptyList());
        assertFalse(controller.update(unit, 1.0f / 60f));
    }

    private static Unit createUnitAt(float x, float y, float z, float yaw) {
        UnitConfig config = createTestConfig();
        int flags = UnitFlags.fromUnitConfig(config);
        Node spatial = new Node("TestUnit");
        Unit unit = new Unit(1, config, spatial, flags);
        unit.setPosition(new Vector3f(x, y, z));
        unit.setBodyYaw(yaw);
        return unit;
    }

    private static UnitConfig createTestConfig() {
        return new UnitConfig(
                new IdentitySection("Test", "Test", "AFV", "Republic",
                        false, false, true, true, true, false, false, false, "unit"),
                new StatsSection(100, "light", 5, 8f, 5f, 5f, 100, 10, 1, -1, 10f),
                null, 0,
                new VeterancySection(false, null, null),
                new CombatSection("", "", "", "", -1, 0f, false, 64, false, false, false),
                new AbilitiesSection(
                        new AbilitiesSection.DeploySection(false, 0f, null),
                        new AbilitiesSection.DockingAbilitySection(false, "", 0f, 0)),
                new PassengersSection(0),
                new AudioSection(null, null, null, null, null, null, ""),
                new MovementSection("tracks", false, true, false),
                new TurretsSection(false, false, null, 0f, null, 0f),
                new BuildingInteractionsSection("", "", false, false, false),
                new SpecialFlagsSection(false));
    }
}
