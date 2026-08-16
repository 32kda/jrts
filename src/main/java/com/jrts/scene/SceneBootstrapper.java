package com.jrts.scene;

import com.jrts.animation.AnimationController;
import com.jrts.building.Building;
import com.jrts.building.BuildingAnimationControl;
import com.jrts.building.BuildingFactory;
import com.jrts.building.BuildingSmokeControl;
import com.jrts.building.ProductionControl;
import com.jrts.config.BuildingConfig;
import com.jrts.config.ConfigLoader;
import com.jrts.config.UnitConfig;
import com.jrts.map.MapBuilding;
import com.jrts.map.MapUnit;
import com.jrts.movement.NavigationService;
import com.jrts.rendering.LoadedModel;
import com.jrts.rendering.ModelLoader;
import com.jrts.turret.TurretConfig;
import com.jrts.turret.TurretControl;
import com.jrts.unit.Unit;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Sets up the scene: terrain, lighting, sky, and spawns units/buildings from map placements.
 *
 * Receives its {@link TerrainHeightProvider} from the caller (the map), keeping terrain a
 * single source of truth rather than constructing one here.
 */
public class SceneBootstrapper {

    private static final Logger log = LoggerFactory.getLogger(SceneBootstrapper.class);

    private final Node rootNode;
    private final AssetManager assetManager;
    private final ViewPort viewPort;
    private final ConfigLoader configLoader;
    private final ModelLoader modelLoader;
    private final UnitFactory unitFactory;
    private final BuildingFactory buildingFactory;

    private final TerrainHeightProvider terrainProvider;

    private NavigationService navigationService;

    public SceneBootstrapper(Node rootNode, AssetManager assetManager, ViewPort viewPort,
                             ConfigLoader configLoader, ModelLoader modelLoader,
                             UnitFactory unitFactory, BuildingFactory buildingFactory,
                             TerrainHeightProvider terrainProvider) {
        this.rootNode = rootNode;
        this.assetManager = assetManager;
        this.viewPort = viewPort;
        this.configLoader = configLoader;
        this.modelLoader = modelLoader;
        this.unitFactory = unitFactory;
        this.buildingFactory = buildingFactory;
        this.terrainProvider = terrainProvider;
        log.info("SceneBootstrapper initialized");
    }

    /**
     * Supplies the navigation service used by production (spawn→exit movement). May be set
     * after construction; production is disabled until it is provided.
     */
    public void setNavigationService(NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    public TerrainHeightProvider getTerrainProvider() {
        return terrainProvider;
    }

    /**
     * Full bootstrap: creates terrain, lights, and sky. Unit/building spawning is driven by
     * map placements via {@link #spawnUnits} and {@link #spawnBuildings}.
     */
    public void bootstrap() {
        log.info("Bootstrapping scene...");
        createTerrain();
        createLighting();
        createSky();
        log.info("Scene bootstrap complete");
    }

    private void createTerrain() {
        float width = terrainProvider.getMapMaxX() - terrainProvider.getMapMinX();
        float depth = terrainProvider.getMapMaxZ() - terrainProvider.getMapMinZ();
        float spacing = Math.max(1f, width / 20f);
        TerrainGrid grid = new TerrainGrid(width, depth, spacing,
                terrainProvider.getHeight(0, 0), assetManager);
        rootNode.attachChild(grid.getGroundGeometry());
        rootNode.attachChild(grid.getGridGeometry());
        log.info("Terrain grid created: {}x{} with {}m spacing", width, depth, spacing);
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

    /**
     * Spawn every unit placement from the map, skipping placements whose config/model is
     * missing.
     */
    public void spawnUnits(List<MapUnit> placements) {
        for (MapUnit placement : placements) {
            spawnUnit(placement);
        }
    }

    /**
     * Spawn a single unit placement from the map.
     */
    public void spawnUnit(MapUnit placement) {
        try {
            UnitConfig config = configLoader.loadUnitConfig(placement.type());
            Vector3f position = new Vector3f(placement.posX(), placement.posY(), placement.posZ());
            spawnUnit(config, position, placement.yaw(), placement.owner(), placement.health());
        } catch (Exception e) {
            log.warn("Could not spawn unit '{}': {}", placement.type(), e.getMessage());
        }
    }

    /**
     * Spawn every building placement from the map.
     */
    public void spawnBuildings(List<MapBuilding> placements) {
        for (MapBuilding placement : placements) {
            spawnBuilding(placement);
        }
    }

    /**
     * Spawn a single building placement from the map.
     */
    public void spawnBuilding(MapBuilding placement) {
        try {
            BuildingConfig config = configLoader.loadBuildingConfig(placement.type());
            LoadedModel model = loadBuildingModel();
            Vector3f position = new Vector3f(placement.posX(), placement.posY(), placement.posZ());
            Building building = buildingFactory.create(config, model, position, placement.yaw());
            building.setOwner(placement.owner());
            if (placement.health() != null) {
                building.setHealth(placement.health());
            }
            rootNode.attachChild(model.spatial());
            attachBuildingControls(building);
        } catch (Exception e) {
            log.warn("Could not spawn building '{}': {}", placement.type(), e.getMessage());
        }
    }

    /**
     * Attach the per-behaviour controls a building declares via its config: defensive turret,
     * production, animation, and smoke.
     */
    private void attachBuildingControls(Building building) {
        BuildingConfig config = building.getConfig();
        Node spatial = building.getSpatial();

        if (config.hasTurret() && building.getTurretPivot() != null) {
            TurretConfig turretConfig = TurretConfig.from(config.turrets());
            spatial.addControl(new TurretControl(turretConfig, building.getTurretPivot(),
                    building.getBarrelPivot()));
            log.info("Attached defensive turret to building {}", building.getId());
        }

        if (!config.production().produces().isEmpty() && navigationService != null) {
            ProductionControl production = new ProductionControl(building,
                    (type, pos, yaw) -> {
                        try {
                            return spawnUnit(configLoader.loadUnitConfig(type), pos, yaw);
                        } catch (Exception e) {
                            log.warn("Could not produce '{}': {}", type, e.getMessage());
                            return null;
                        }
                    },
                    navigationService);
            spatial.addControl(production);
            log.info("Attached production to building {} (queue {})",
                    building.getId(), config.production().produces().size());
        }

        spatial.addControl(new BuildingAnimationControl(new AnimationController()));
        spatial.addControl(new BuildingSmokeControl(building, assetManager));
    }

    /**
     * Loads a model, creates a unit from it, and attaches it to the scene.
     *
     * @param config   parsed TOML unit config
     * @param position initial world position
     * @param yaw      initial body facing (radians)
     * @return the newly created (and attached) unit
     */
    public Unit spawnUnit(UnitConfig config, Vector3f position, float yaw) {
        return spawnUnit(config, position, yaw, null, null);
    }

    /**
     * Creates a unit with explicit in-game properties (owner, health).
     */
    public Unit spawnUnit(UnitConfig config, Vector3f position, float yaw,
                          Integer owner, Integer health) {
        LoadedModel loadedModel = loadModel();
        Unit unit = unitFactory.create(config, loadedModel, position, yaw);
        if (owner != null) {
            unit.setOwner(owner);
        }
        if (health != null) {
            unit.setHealth(health);
        }
        rootNode.attachChild(loadedModel.spatial());
        return unit;
    }

    private LoadedModel loadModel() {
        Path[] candidatePaths = {
                Path.of("assets/models/final/nod_heavy_tank.m3o"),
                Path.of("assets/models/final/heavy_tank.m3o")
        };

        for (Path path : candidatePaths) {
            if (Files.exists(path)) {
                try {
                    LoadedModel model = modelLoader.loadM3o(path);
                    log.info("Loaded .m3o model from {}", path);
                    return model;
                } catch (Exception e) {
                    log.warn("Failed to load .m3o from {}: {}", path, e.getMessage());
                }
            }
        }

        log.warn("No .m3o model found, using placeholder");
        return new LoadedModel(createPlaceholderTankNode(), null);
    }

    private LoadedModel loadBuildingModel() {
        return new LoadedModel(createPlaceholderBuildingNode(), null);
    }

    /**
     * Creates a simple geometric placeholder for the tank.
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

    /**
     * Creates a simple geometric placeholder for a building.
     */
    private Node createPlaceholderBuildingNode() {
        Node node = new Node("Building_Placeholder");

        com.jme3.scene.shape.Box body = new com.jme3.scene.shape.Box(6f, 4f, 6f);
        com.jme3.scene.Geometry bodyGeom = new com.jme3.scene.Geometry("BuildingBody", body);
        bodyGeom.setLocalTranslation(0, 4f, 0);
        com.jme3.material.Material mat = new com.jme3.material.Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Diffuse", new ColorRGBA(0.45f, 0.45f, 0.5f, 1.0f));
        bodyGeom.setMaterial(mat);
        node.attachChild(bodyGeom);

        return node;
    }
}
