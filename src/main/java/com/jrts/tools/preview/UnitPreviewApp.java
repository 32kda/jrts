package com.jrts.tools.preview;

import com.jrts.rendering.LoadedModel;
import com.jrts.rendering.ModelLoader;
import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.*;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Standalone JME application for artist/developer model preview.
 * Launched via: ./gradlew previewModel --args="path/to/model.m3o"
 *
 * Features:
 *   - Orbit camera (left-drag rotate, scroll zoom, right-drag pan)
 *   - Three-point studio lighting
 *   - Reference floor grid
 *   - Turret yaw/pitch controls (keyboard + auto-scan)
 *   - Muzzle flash particle test
 *   - Collision wireframe overlay toggle
 */
public class UnitPreviewApp extends SimpleApplication {

    private static final Logger log = LoggerFactory.getLogger(UnitPreviewApp.class);

    private OrbitCamera orbitCamera;
    private TurretPreviewController turretController;
    private ParticlePreviewEmitter particleEmitter;
    private CollisionWireframe collisionWireframe;
    private String currentModelPath;

    private boolean leftPressed;
    private boolean rightPressed;
    private Node loadedModelNode;

    public static void main(String[] args) {
        UnitPreviewApp app = new UnitPreviewApp();
        if (args.length > 0) {
            app.currentModelPath = args[0];
        }
        app.start();
    }

    @Override
    public void simpleInitApp() {
        log.info("Unit Preview App starting...");

        flyCam.setEnabled(false);

        createLighting();
        createFloorGrid();

        orbitCamera = new OrbitCamera(cam);
        turretController = new TurretPreviewController();
        particleEmitter = new ParticlePreviewEmitter(rootNode, assetManager);
        collisionWireframe = new CollisionWireframe(rootNode, assetManager);

        registerInput();

        if (currentModelPath != null) {
            loadModel(currentModelPath);
        }

        log.info("Unit Preview App ready");
    }

    private void createLighting() {
        DirectionalLight keyLight = new DirectionalLight();
        keyLight.setDirection(new Vector3f(-0.6f, -0.8f, -0.5f).normalizeLocal());
        keyLight.setColor(new ColorRGBA(1f, 0.98f, 0.92f, 1f));
        rootNode.addLight(keyLight);

        DirectionalLight fillLight = new DirectionalLight();
        fillLight.setDirection(new Vector3f(0.3f, -0.4f, 0.2f).normalizeLocal());
        fillLight.setColor(new ColorRGBA(0.4f, 0.45f, 0.5f, 1f));
        rootNode.addLight(fillLight);

        AmbientLight ambient = new AmbientLight();
        ambient.setColor(new ColorRGBA(0.3f, 0.3f, 0.35f, 1f));
        rootNode.addLight(ambient);
    }

    private void createFloorGrid() {
        float size = 20f;
        Quad quad = new Quad(size, size);
        Geometry floorGeom = new Geometry("FloorGrid", quad);
        floorGeom.rotate(-(float) Math.PI / 2, 0, 0);
        floorGeom.setLocalTranslation(-size / 2, -0.01f, -size / 2);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(0.3f, 0.3f, 0.3f, 1f));
        floorGeom.setMaterial(mat);

        rootNode.attachChild(floorGeom);
    }

    private void registerInput() {
        inputManager.addMapping("ORBIT_LEFT", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("ORBIT_RIGHT", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addMapping("ZOOM_IN", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        inputManager.addMapping("ZOOM_OUT", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
        inputManager.addMapping("AUTOSCAN", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("FIRE", new KeyTrigger(KeyInput.KEY_F));
        inputManager.addMapping("WIREFRAME", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("RESET", new KeyTrigger(KeyInput.KEY_R));
        inputManager.addMapping("YAW_LEFT", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("YAW_RIGHT", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("PITCH_UP", new KeyTrigger(KeyInput.KEY_Q));
        inputManager.addMapping("PITCH_DOWN", new KeyTrigger(KeyInput.KEY_E));

        inputManager.addListener(actionListener,
                "ORBIT_LEFT", "ORBIT_RIGHT",
                "ZOOM_IN", "ZOOM_OUT",
                "AUTOSCAN", "FIRE", "WIREFRAME", "RESET",
                "YAW_LEFT", "YAW_RIGHT", "PITCH_UP", "PITCH_DOWN");
    }

    private boolean yawLeft;
    private boolean yawRight;
    private boolean pitchUp;
    private boolean pitchDown;

    private final ActionListener actionListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            switch (name) {
                case "ORBIT_LEFT" -> leftPressed = isPressed;
                case "ORBIT_RIGHT" -> rightPressed = isPressed;
                case "ZOOM_IN" -> {
                    if (isPressed) orbitCamera.onScroll(1f);
                }
                case "ZOOM_OUT" -> {
                    if (isPressed) orbitCamera.onScroll(-1f);
                }
                case "AUTOSCAN" -> {
                    if (isPressed && turretController != null) {
                        turretController.setAutoScan(!turretController.isAutoScan());
                        log.info("Auto-scan: {}", turretController.isAutoScan());
                    }
                }
                case "FIRE" -> {
                    if (isPressed) particleEmitter.emitBurst();
                }
                case "WIREFRAME" -> {
                    if (isPressed) collisionWireframe.toggle();
                }
                case "RESET" -> {
                    if (isPressed) orbitCamera.reset();
                }
                case "YAW_LEFT" -> yawLeft = isPressed;
                case "YAW_RIGHT" -> yawRight = isPressed;
                case "PITCH_UP" -> pitchUp = isPressed;
                case "PITCH_DOWN" -> pitchDown = isPressed;
            }
        }
    };

    @Override
    public void simpleUpdate(float tpf) {
        int mouseX = (int) inputManager.getCursorPosition().x;
        int mouseY = (int) inputManager.getCursorPosition().y;
        orbitCamera.onMouseMove(mouseX, mouseY, leftPressed, rightPressed);
        orbitCamera.update(tpf);

        if (turretController != null) {
            if (yawLeft) {
                turretController.setYawDegrees(turretController.getCurrentYawDeg() - 2f);
            }
            if (yawRight) {
                turretController.setYawDegrees(turretController.getCurrentYawDeg() + 2f);
            }
            if (pitchUp) {
                turretController.setPitchDegrees(turretController.getCurrentPitchDeg() - 1f);
            }
            if (pitchDown) {
                turretController.setPitchDegrees(turretController.getCurrentPitchDeg() + 1f);
            }
            turretController.update(tpf);
        }
    }

    public void loadModel(String m3oPath) {
        log.info("Loading model: {}", m3oPath);
        unloadModel();

        try {
            Path path = Path.of(m3oPath);
            ModelLoader loader = new ModelLoader();
            LoadedModel model = loader.loadM3o(path);

            loadedModelNode = model.spatial();
            rootNode.attachChild(loadedModelNode);

            if (model.manifest() != null && model.manifest().getCollision() != null) {
                var collision = model.manifest().getCollision();
                if ("rect".equals(collision.type())) {
                    collisionWireframe.showBox(collision.center(), collision.halfExtents());
                }
            }

            orbitCamera.setLookAt(new Vector3f(0, 2, 0));
            orbitCamera.reset();

            log.info("Model loaded: {}", model.manifest() != null
                    ? model.manifest().getModelName() : m3oPath);
        } catch (Exception e) {
            log.error("Failed to load model: {}", m3oPath, e);
        }
    }

    public void unloadModel() {
        if (loadedModelNode != null) {
            loadedModelNode.removeFromParent();
            loadedModelNode = null;
        }
        collisionWireframe.hide();
    }
}
