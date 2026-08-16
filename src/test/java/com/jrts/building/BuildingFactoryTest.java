package com.jrts.building;

import com.jrts.config.BuildingConfig;
import com.jrts.config.ConfigLoader;
import com.jrts.rendering.LoadedModel;
import com.jrts.tools.importer.ModelManifest;
import com.jrts.tools.importer.NodeRole;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BuildingFactoryTest {

    @Test
    void resolvesBuildingRolesFromManifest() {
        Node root = new Node("BuildingRoot");
        Node turretPivot = new Node("TurretPivot");
        Node spawnPoint = new Node("SpawnPoint");
        Node exitPoint = new Node("ExitPoint");
        root.attachChild(turretPivot);
        root.attachChild(spawnPoint);
        root.attachChild(exitPoint);

        ModelManifest manifest = new ModelManifest();
        manifest.registerRole(NodeRole.TURRET_PIVOT, "TurretPivot");
        manifest.registerRole(NodeRole.SPAWN_POINT, "SpawnPoint");
        manifest.registerRole(NodeRole.EXIT_POINT, "ExitPoint");

        BuildingConfig config = new ConfigLoader(Path.of("src/test/resources/config"))
                .loadBuildingConfig("building_example");
        BuildingFactory factory = new BuildingFactory(new BuildingRegistry());

        Building building = factory.create(config, new LoadedModel(root, manifest),
                new Vector3f(0f, 0f, 0f), 0f);

        assertSame(turretPivot, building.getTurretPivot());
        assertSame(spawnPoint, building.getSpawnPoint());
        assertSame(exitPoint, building.getExitPoint());
        assertNull(building.getDockPoint(), "unregistered role should remain null");
        assertEquals(1200, building.getHealth());
    }

    @Test
    void nullManifestLeavesRolesUnset() {
        BuildingConfig config = new ConfigLoader(Path.of("src/test/resources/config"))
                .loadBuildingConfig("building_example");
        BuildingFactory factory = new BuildingFactory(new BuildingRegistry());

        Building building = factory.create(config, new LoadedModel(new Node("Root"), null),
                new Vector3f(0f, 0f, 0f), 0f);

        assertNull(building.getTurretPivot());
        assertNull(building.getSpawnPoint());
    }
}
