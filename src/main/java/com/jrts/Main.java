package com.jrts;

import com.jrts.camera.RtsCamera;
import com.jrts.camera.RtsCameraInputListener;
import com.jrts.config.ConfigLoader;
import com.jrts.input.*;
import com.jrts.movement.MovementController;
import com.jrts.movement.NavigationService;
import com.jrts.movement.SimpleLineNavigation;
import com.jrts.movement.TerrainSnapping;
import com.jrts.rendering.ModelLoader;
import com.jrts.scene.FlatTerrainHeightProvider;
import com.jrts.scene.SceneBootstrapper;
import com.jrts.scene.TerrainHeightProvider;
import com.jrts.selectable.SelectionHighlight;
import com.jrts.turret.TurretController;
import com.jrts.unit.Unit;
import com.jrts.unit.UnitFactory;
import com.jrts.unit.UnitRegistry;
import com.jme3.app.SimpleApplication;
import com.jme3.math.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Main entry point. Extends JME's SimpleApplication.
 *
 * Orchestrates initialization, wires all subsystems together,
 * and runs the main update loop.
 *
 * This is the ONLY class that "knows about everything" — it acts as
 * the composition root (DI container). It does NOT contain business logic.
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
    private MovementController movementController;
    private TerrainSnapping terrainSnapping;
    private TurretController turretController;
    private NavigationService navigationService;
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
        unitFactory = new UnitFactory(unitRegistry);

        float mapSize = 500f;
        terrainProvider = new FlatTerrainHeightProvider(0f, mapSize);

        movementController = new MovementController(terrainProvider);
        terrainSnapping = new TerrainSnapping(terrainProvider);
        navigationService = new SimpleLineNavigation(terrainProvider);

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

        turretController = new TurretController();

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
                unitFactory, mapSize);
        sceneBootstrapper.bootstrap();

        log.info("=== Dune RTS Stage 1 Ready ===");
    }

    @Override
    public void simpleUpdate(float tpf) {
        rtsCamera.update(tpf);

        int mouseX = (int) inputManager.getCursorPosition().x;
        int mouseY = (int) inputManager.getCursorPosition().y;
        cameraInput.update(tpf, mouseX, mouseY);

        Vector3f cursorWorldPos = mousePicker.pickTerrain(mouseX, mouseY).orElse(null);

        terrainSnapping.clampAll(unitRegistry);

        for (Unit unit : selectionSystem.getSelected()) {
            if (unit.hasTurret() && cursorWorldPos != null) {
                turretController.update(unit, cursorWorldPos, tpf);
            }

            if (unit.canMove() && unit.getWaypoints() != null
                    && !unit.getWaypoints().isEmpty()) {
                movementController.update(unit, tpf);
            }
        }
    }
}
