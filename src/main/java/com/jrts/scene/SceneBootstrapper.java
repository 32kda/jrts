package com.jrts.scene;

import com.jrts.config.ConfigLoader;
import com.jrts.config.UnitConfig;
import com.jrts.rendering.LoadedModel;
import com.jrts.rendering.ModelLoader;
import com.jrts.unit.UnitFactory;
import com.jme3.asset.AssetManager;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sets up the Stage 1 scene: terrain, lighting, sky, and initial units.
 *
 * Called once at app startup.
 * Delegates to focused helper methods rather than doing everything inline.
 *
 * Creates a FlatTerrainHeightProvider (constant height Y=0) for Stage 1.
 * When real terrain is ready in Stage 2+, swap to HeightmapTerrainProvider
 * — the interface is identical.
 */
public class SceneBootstrapper {

    private static final Logger log = LoggerFactory.getLogger(SceneBootstrapper.class);

    private final Node rootNode;
    private final AssetManager assetManager;
    private final ViewPort viewPort;
    private final ConfigLoader configLoader;
    private final ModelLoader modelLoader;
    private final UnitFactory unitFactory;

    private final TerrainHeightProvider terrainProvider;

    public SceneBootstrapper(Node rootNode, AssetManager assetManager, ViewPort viewPort,
                             ConfigLoader configLoader, ModelLoader modelLoader,
                             UnitFactory unitFactory, float mapSize) {
        this.rootNode = rootNode;
        this.assetManager = assetManager;
        this.viewPort = viewPort;
        this.configLoader = configLoader;
        this.modelLoader = modelLoader;
        this.unitFactory = unitFactory;
        this.terrainProvider = new FlatTerrainHeightProvider(0f, mapSize);
        log.info("SceneBootstrapper initialized with mapSize={}", mapSize);
    }

    /**
     * @return the terrain provider (needed by other systems)
     */
    public TerrainHeightProvider getTerrainProvider() {
        return terrainProvider;
    }

    /**
     * Full bootstrap: creates terrain, lights, then spawns initial units.
     */
    public void bootstrap() {
        log.info("Bootstrapping scene...");
        createTerrain();
        createLighting();
        createSky();
        spawnInitialUnits();
        log.info("Scene bootstrap complete");
    }

    private void createTerrain() {
        TerrainGrid grid = new TerrainGrid(1000f, 1000f, 50f,
                terrainProvider.getHeight(0, 0), assetManager);
        rootNode.attachChild(grid.getGroundGeometry());
        rootNode.attachChild(grid.getGridGeometry());
        log.info("Terrain grid created: 1000x1000 with 50m spacing");
    }

    private void createLighting() {
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.5f, -0.8f, -0.3f).normalizeLocal());
        sun.setColor(new ColorRGBA(1.0f, 0.95f, 0.85f, 1.0f));
        rootNode.addLight(sun);

        AmbientLight ambient = new AmbientLight();
        ambient.setColor(new ColorRGBA(0.3f, 0.3f, 0.35f, 1.0f));
        rootNode.addLight(ambient);

        log.info("Lighting created: directional sun + ambient");
    }

    private void createSky() {
        viewPort.setBackgroundColor(new ColorRGBA(0.35f, 0.55f, 0.30f, 1.0f));
        log.info("Sky color set (green background)");
    }

    private void spawnInitialUnits() {
        try {
            UnitConfig tankConfig = configLoader.loadUnitConfig("heavy_tank");

            Vector3f spawnPos = new Vector3f(0, 0, 0);
            spawnPos.y = terrainProvider.getHeight(spawnPos.x, spawnPos.z);

            Node placeholderNode = createPlaceholderTankNode();
            LoadedModel placeholderModel = new LoadedModel(placeholderNode, null);

            unitFactory.create(tankConfig, placeholderModel, spawnPos, 0.5f);

            rootNode.attachChild(placeholderNode);

            log.info("Spawned 1 HeavyTank at origin");
        } catch (Exception e) {
            log.warn("Could not spawn tank - config or model missing: {}", e.getMessage());
        }
    }

    /**
     * Creates a simple geometric placeholder for the tank.
     * In production, this would be a loaded .m3o model.
     */
    private Node createPlaceholderTankNode() {
        Node node = new Node("HeavyTank_Placeholder");

        com.jme3.scene.shape.Box body = new com.jme3.scene.shape.Box(1.5f, 0.8f, 3f);
        com.jme3.scene.Geometry bodyGeom = new com.jme3.scene.Geometry("Chassis", body);
        com.jme3.material.Material bodyMat = new com.jme3.material.Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        bodyMat.setBoolean("UseMaterialColors", true);
        bodyMat.setColor("Diffuse", new ColorRGBA(0.4f, 0.35f, 0.25f, 1.0f));
        bodyGeom.setMaterial(bodyMat);
        node.attachChild(bodyGeom);

        Node turretPivot = new Node("TurretPivot");
        turretPivot.setLocalTranslation(0, 0.9f, 0);
        node.attachChild(turretPivot);

        com.jme3.scene.shape.Box turret = new com.jme3.scene.shape.Box(1f, 0.5f, 1.5f);
        com.jme3.scene.Geometry turretGeom = new com.jme3.scene.Geometry("Turret", turret);
        com.jme3.material.Material turretMat = new com.jme3.material.Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        turretMat.setBoolean("UseMaterialColors", true);
        turretMat.setColor("Diffuse", new ColorRGBA(0.3f, 0.3f, 0.25f, 1.0f));
        turretGeom.setMaterial(turretMat);
        turretPivot.attachChild(turretGeom);

        com.jme3.scene.shape.Cylinder barrel = new com.jme3.scene.shape.Cylinder(8, 16,
                0.15f, 3f);
        com.jme3.scene.Geometry barrelGeom = new com.jme3.scene.Geometry("Barrel", barrel);
        barrelGeom.rotate(-(float) Math.PI / 2, 0, 0);
        barrelGeom.setLocalTranslation(0, 0, 1.5f);
        com.jme3.material.Material barrelMat = new com.jme3.material.Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        barrelMat.setBoolean("UseMaterialColors", true);
        barrelMat.setColor("Diffuse", ColorRGBA.Gray);
        barrelGeom.setMaterial(barrelMat);
        turretPivot.attachChild(barrelGeom);

        return node;
    }
}
