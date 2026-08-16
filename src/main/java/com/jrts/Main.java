package com.jrts;

import com.jrts.building.BuildingFactory;
import com.jrts.building.BuildingRegistry;
import com.jrts.camera.RtsCamera;
import com.jrts.camera.RtsCameraInputListener;
import com.jrts.config.ConfigLoader;
import com.jrts.input.*;
import com.jrts.map.MapDefinition;
import com.jrts.map.MapLoader;
import com.jrts.movement.AStarNavigation;
import com.jrts.movement.LocalAvoidance;
import com.jrts.movement.NavigationService;
import com.jrts.movement.TerrainSnapping;
import com.jrts.pathfinding.HpaPathfinder;
import com.jrts.pathfinding.PathSmoother;
import com.jrts.pathfinding.SurfaceMask;
import com.jrts.pathfinding.TraversalProfile;
import com.jrts.rendering.ModelLoader;
import com.jrts.scene.ObstacleRenderer;
import com.jrts.scene.SceneBootstrapper;
import com.jrts.scene.TerrainHeightProvider;
import com.jrts.scene.TestMap;
import com.jrts.selectable.SelectionHighlight;
import com.jrts.turret.TurretControl;
import com.jrts.unit.Unit;
import com.jrts.unit.UnitFactory;
import com.jrts.unit.UnitRegistry;
import com.jme3.app.SimpleApplication;
import com.jme3.math.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Main entry point. Extends JME's SimpleApplication.
 *
 * This is the composition root (DI container): it wires subsystems together and owns the
 * update loop. It contains no business logic — the map is loaded from a JSON file by
 * {@link MapLoader}, the navigation algorithm by {@link com.jrts.pathfinding}, and per-unit
 * behaviour (movement, turret) runs via JME Controls attached to each spatial (Composite).
 */
public class Main extends SimpleApplication {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private RtsCamera rtsCamera;
    private RtsCameraInputListener cameraInput;
    private ActionMapper actionMapper;
    private MousePicker mousePicker;
    private SelectionBox selectionBox;
    private SelectionSystem selectionSystem;
    private SelectionHighlight selectionHighlight;
    private CommandDispatcher commandDispatcher;
    private TerrainSnapping terrainSnapping;
    private NavigationService navigationService;
    private LocalAvoidance localAvoidance;
    private TerrainHeightProvider terrainProvider;
    private UnitFactory unitFactory;
    private UnitRegistry unitRegistry;

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        log.info("=== Dune RTS Stage 1 Initializing ===");

        flyCam.setEnabled(false);

        unitRegistry = new UnitRegistry();
        BuildingRegistry buildingRegistry = new BuildingRegistry();

        MapDefinition map = loadMap();
        terrainProvider = map.terrain();

        unitFactory = new UnitFactory(unitRegistry, terrainProvider);
        BuildingFactory buildingFactory = new BuildingFactory(buildingRegistry);

        TraversalProfile tankProfile = new TraversalProfile(SurfaceMask.GROUND, 1f, 2);
        navigationService = new AStarNavigation(map.grid(), new HpaPathfinder(), new PathSmoother(),
                terrainProvider, tankProfile);
        localAvoidance = new LocalAvoidance(3f);
        terrainSnapping = new TerrainSnapping(terrainProvider);

        rtsCamera = new RtsCamera(cam);
        rtsCamera.setTerrainProvider(terrainProvider);
        rtsCamera.update(0f);

        mousePicker = new MousePicker(rootNode, unitRegistry, cam, terrainProvider);
        selectionBox = new SelectionBox();
        selectionSystem = new SelectionSystem(unitRegistry, mousePicker, selectionBox, cam,
                settings.getWidth(), settings.getHeight());
        selectionHighlight = new SelectionHighlight(rootNode, assetManager);
        selectionSystem.addObserver(selectionHighlight);

        commandDispatcher = new CommandDispatcher(selectionSystem, mousePicker, navigationService);

        actionMapper = new ActionMapper();
        actionMapper.addHandler(selectionSystem);
        actionMapper.addHandler(commandDispatcher);
        inputManager.addRawInputListener(actionMapper);

        cameraInput = new RtsCameraInputListener(rtsCamera,
                settings.getWidth(), settings.getHeight());
        cameraInput.registerWith(inputManager);

        Path configPath = Path.of("assets/config");
        ConfigLoader configLoader = new ConfigLoader(configPath);
        ModelLoader modelLoader = new ModelLoader();

        SceneBootstrapper sceneBootstrapper = new SceneBootstrapper(
                rootNode, assetManager, viewPort, configLoader, modelLoader,
                unitFactory, buildingFactory, terrainProvider);
        sceneBootstrapper.setNavigationService(navigationService);
        sceneBootstrapper.bootstrap();
        sceneBootstrapper.spawnUnits(map.units());
        sceneBootstrapper.spawnBuildings(map.buildings());

        new ObstacleRenderer(rootNode, assetManager).render(map.obstacles());

        log.info("=== Dune RTS Stage 1 Ready ===");
    }

    private MapDefinition loadMap() {
        Path mapFile = Path.of("assets/maps/test_map.json");
        try {
            return new MapLoader().load(mapFile);
        } catch (Exception e) {
            log.error("Failed to load map {}, falling back to TestMap", mapFile, e);
            TestMap testMap = new TestMap();
            return new MapDefinition(testMap.terrain(), testMap.grid(), testMap.obstacles(),
                    List.of(), List.of(), List.of(), List.of());
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        rtsCamera.update(tpf);

        int mouseX = (int) inputManager.getCursorPosition().x;
        int mouseY = (int) inputManager.getCursorPosition().y;
        cameraInput.update(tpf, mouseX, mouseY);

        Vector3f cursorWorldPos = mousePicker.pickTerrain(mouseX, mouseY).orElse(null);

        // Cross-unit systems. Per-unit movement and turret behaviour run via JME Controls
        // attached to each spatial (Composite pattern), driven by the scene-graph traversal.
        terrainSnapping.clampAll(unitRegistry);
        localAvoidance.separate(unitRegistry.allUnits());

        // Feed aim targets into each unit's turret control.
        Set<Unit> selected = selectionSystem.getSelected();
        for (Unit unit : unitRegistry.allUnits()) {
            TurretControl control = unit.getSpatial().getControl(TurretControl.class);
            if (control == null) {
                continue;
            }
            if (selected.contains(unit) && cursorWorldPos != null) {
                control.setTarget(cursorWorldPos);
            } else {
                control.clearTarget();
            }
        }
    }
}
