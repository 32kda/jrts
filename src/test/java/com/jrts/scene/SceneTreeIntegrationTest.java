package com.jrts.scene;

import com.jrts.config.UnitConfig;
import com.jrts.unit.Unit;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that bootstrapping a scene produces a correct scene tree:
 * terrain (Ground + Grid) and a single tank whose hierarchy contains
 * all desired nodes (chassis → TurretPivot → turret → Muzzle).
 */
class SceneTreeIntegrationTest {

    private SceneTestSupport ctx;

    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(Files.exists(SceneTestSupport.MODEL_PATH),
                "Run importModels first to generate the .m3o model");
        ctx = SceneTestSupport.create();
        ctx.bootstrapper.bootstrap();
        UnitConfig config = ctx.configLoader.loadUnitConfig("heavy_tank");
        ctx.bootstrapper.spawnUnit(config, new Vector3f(0f, 0f, 0f), 0f);
    }

    @Test
    void sceneContainsTerrainAndSingleTankWithFullHierarchy() {
        Node root = ctx.rootNode;

        assertNotNull(findChild(root, "Ground"), "Ground geometry missing");
        assertNotNull(findChild(root, "Grid"), "Grid geometry missing");

        assertEquals(1, ctx.registry.count());

        Unit tank = ctx.registry.findById(1).orElseThrow();
        Node tankRoot = tank.getSpatial();

        assertTrue(root.getChildren().contains(tankRoot), "tank root not attached to scene");

        Node chassis = assertNodeChild(tankRoot, "chassis");
        Node turretPivot = assertNodeChild(chassis, "TurretPivot");
        Node turret = assertNodeChild(turretPivot, "turret");
        assertNodeChild(turret, "Muzzle");

        assertNotNull(tank.getTurretPivot(), "turret pivot not resolved from manifest");
        assertNotNull(tank.getMuzzle(), "muzzle not resolved from manifest");

        assertTrue(ctx.registry.findByEntityId(tankRoot).isPresent());
        assertSame(tank, ctx.registry.findByEntityId(tankRoot).get());
    }

    @Test
    void tankHasExpectedFlags() {
        Unit tank = ctx.registry.findById(1).orElseThrow();
        assertTrue(tank.isSelectable());
        assertTrue(tank.hasTurret());
        assertTrue(tank.canMove());
    }

    private static Spatial findChild(Node parent, String name) {
        for (Spatial child : parent.getChildren()) {
            if (name.equals(child.getName())) {
                return child;
            }
        }
        return null;
    }

    private static Node assertNodeChild(Node parent, String name) {
        Spatial child = findChild(parent, name);
        assertNotNull(child, "Expected child '" + name + "' under '" + parent.getName() + "'");
        assertInstanceOf(Node.class, child, "Expected '" + name + "' to be a Node");
        return (Node) child;
    }
}
