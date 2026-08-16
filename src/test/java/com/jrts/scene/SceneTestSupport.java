package com.jrts.scene;

import com.jrts.building.BuildingFactory;
import com.jrts.building.BuildingRegistry;
import com.jrts.config.ConfigLoader;
import com.jrts.rendering.ModelLoader;
import com.jrts.unit.UnitFactory;
import com.jrts.unit.UnitRegistry;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.material.plugins.J3MLoader;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.system.JmeSystem;
import com.jme3.texture.plugins.AWTLoader;

import java.nio.file.Path;

/**
 * Builds a headless JME scene stack for integration tests.
 * Mirrors the wiring in {@link com.jrts.Main#simpleInitApp()} but without
 * any GL context / renderer / input.
 */
final class SceneTestSupport {

    static final Path MODEL_PATH = Path.of("assets/models/final/nod_heavy_tank.m3o");

    final Node rootNode;
    final ConfigLoader configLoader;
    final UnitRegistry registry;
    final UnitFactory unitFactory;
    final SceneBootstrapper bootstrapper;

    private SceneTestSupport(Node rootNode, ConfigLoader configLoader, UnitRegistry registry,
                             UnitFactory unitFactory, SceneBootstrapper bootstrapper) {
        this.rootNode = rootNode;
        this.configLoader = configLoader;
        this.registry = registry;
        this.unitFactory = unitFactory;
        this.bootstrapper = bootstrapper;
    }

    static SceneTestSupport create() {
        Node rootNode = new Node("Root");

        JmeSystem.setLowPermissions(false);
        DesktopAssetManager assetManager = new DesktopAssetManager();
        assetManager.registerLocator("/", ClasspathLocator.class);
        assetManager.registerLocator("", FileLocator.class);
        assetManager.registerLoader(J3MLoader.class, "j3md", "j3m");
        assetManager.registerLoader(AWTLoader.class, "png", "jpg", "jpeg", "gif", "bmp");

        ViewPort viewPort = new ViewPort("TestView", new Camera(800, 600));

        ConfigLoader configLoader = new ConfigLoader(Path.of("assets/config"));
        ModelLoader modelLoader = new ModelLoader();

        TerrainHeightProvider terrain = new FlatTerrainHeightProvider(0f, 500f);
        UnitRegistry registry = new UnitRegistry();
        UnitFactory unitFactory = new UnitFactory(registry, terrain);
        BuildingRegistry buildingRegistry = new BuildingRegistry();
        BuildingFactory buildingFactory = new BuildingFactory(buildingRegistry);

        SceneBootstrapper bootstrapper = new SceneBootstrapper(
                rootNode, assetManager, viewPort, configLoader, modelLoader,
                unitFactory, buildingFactory, terrain);

        return new SceneTestSupport(rootNode, configLoader, registry, unitFactory, bootstrapper);
    }
}
