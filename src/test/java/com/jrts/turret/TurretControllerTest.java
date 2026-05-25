package com.jrts.turret;

import com.jrts.config.*;
import com.jrts.unit.Unit;
import com.jrts.unit.UnitFlags;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class TurretControllerTest {

    private TurretController controller;
    private Node turretPivot;

    @BeforeEach
    void setUp() {
        controller = new TurretController();
        turretPivot = new Node("TestTurretPivot");
    }

    @Test
    void turretRotatesTowardTarget() {
        Unit unit = createUnitWithTurret(new Vector3f(0, 0, 0), 0f);

        Vector3f target = new Vector3f(10, 0, 0);
        controller.update(unit, target, 1.0f / 60f);

        float[] angles = turretPivot.getLocalRotation().toAngles(null);
        assertTrue(angles[1] > 0);
    }

    @Test
    void noTurretUnitIsNoOp() {
        Unit unit = createUnitWithoutTurret();

        assertDoesNotThrow(() -> controller.update(unit, new Vector3f(10, 0, 0), 1.0f / 60f));
    }

    @Test
    void nullTurretPivotIsNoOp() {
        Unit unit = createUnitWithNullTurret();

        assertDoesNotThrow(() -> controller.update(unit, new Vector3f(10, 0, 0), 1.0f / 60f));
    }

    private Unit createUnitWithTurret(Vector3f pos, float yaw) {
        Node spatial = new Node("TestSpatial");
        spatial.attachChild(turretPivot);

        UnitConfig config = createTestConfig(true);
        int flags = UnitFlags.fromUnitConfig(config);
        Unit unit = new Unit(1, config, spatial, flags);
        unit.setPosition(pos);
        unit.setBodyYaw(yaw);
        unit.setTurretPivot(turretPivot);
        return unit;
    }

    private Unit createUnitWithoutTurret() {
        Node spatial = new Node("TestSpatial");
        UnitConfig config = createTestConfig(false);
        int flags = UnitFlags.fromUnitConfig(config);
        Unit unit = new Unit(2, config, spatial, flags);
        unit.setPosition(new Vector3f(0, 0, 0));
        return unit;
    }

    private Unit createUnitWithNullTurret() {
        Node spatial = new Node("TestSpatial");
        UnitConfig config = createTestConfig(true);
        int flags = UnitFlags.fromUnitConfig(config);
        Unit unit = new Unit(3, config, spatial, flags);
        unit.setPosition(new Vector3f(0, 0, 0));
        unit.setTurretPivot(null);
        return unit;
    }

    private static UnitConfig createTestConfig(boolean hasTurret) {
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
                new TurretsSection(hasTurret, false,
                        Arrays.asList(-180f, 180f), 5f,
                        Arrays.asList(-5f, 45f), 4f),
                new BuildingInteractionsSection("", "", false, false, false),
                new SpecialFlagsSection(false));
    }
}
