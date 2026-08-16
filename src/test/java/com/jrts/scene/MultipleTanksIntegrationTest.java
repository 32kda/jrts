package com.jrts.scene;

import com.jrts.config.UnitConfig;
import com.jrts.unit.Unit;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that many tanks can be spawned into one scene with unique ids,
 * distinct positions, and each resolving its turret pivot / muzzle nodes.
 */
class MultipleTanksIntegrationTest {

    private SceneTestSupport ctx;

    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(Files.exists(SceneTestSupport.MODEL_PATH),
                "Run importModels first to generate the .m3o model");
        ctx = SceneTestSupport.create();
        ctx.bootstrapper.bootstrap();
    }

    @Test
    void tenTanksSpawnedWithUniqueIdsPositionsAndResolvedNodes() {
        UnitConfig config = ctx.configLoader.loadUnitConfig("heavy_tank");

        for (int i = 0; i < 10; i++) {
            ctx.bootstrapper.spawnUnit(config, new Vector3f(i * 4f, 0, 0), 0f);
        }

        assertEquals(10, ctx.registry.count());

        for (int id = 1; id <= 10; id++) {
            assertTrue(ctx.registry.findById(id).isPresent(), "missing unit id " + id);
        }

        int tankCount = 0;
        for (Spatial child : ctx.rootNode.getChildren()) {
            String name = child.getName();
            if (!"Ground".equals(name) && !"Grid".equals(name)) {
                tankCount++;
            }
        }
        assertEquals(10, tankCount, "expected 10 tank nodes in scene root");

        ctx.rootNode.updateGeometricState();

        Set<String> positions = new HashSet<>();
        for (Unit unit : ctx.registry.allUnits()) {
            assertNotNull(unit.getTurretPivot(), "unit " + unit.getId() + " missing turret pivot");
            assertNotNull(unit.getMuzzle(), "unit " + unit.getId() + " missing muzzle");

            Vector3f p = unit.getPosition();
            positions.add(p.x + "," + p.y + "," + p.z);
        }
        assertEquals(10, positions.size(), "units should have distinct positions");
    }
}
