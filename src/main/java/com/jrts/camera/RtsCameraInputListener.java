package com.jrts.camera;

import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridges JME's raw InputManager events to RtsCamera method calls.
 *
 * Key bindings:
 *   W/A/S/D or Arrow keys → pan
 *   Mouse at screen edge (< 10px) → auto-scroll
 *   Middle mouse drag → rotate yaw
 *   Scroll wheel → zoom
 *   Ctrl+F1..F4 → save bookmark (stub)
 *   F1..F4 → restore bookmark (stub)
 */
public class RtsCameraInputListener implements AnalogListener, ActionListener {

    private static final Logger log = LoggerFactory.getLogger(RtsCameraInputListener.class);

    private final RtsCamera camera;
    private final int screenWidth;
    private final int screenHeight;

    private boolean middleButtonDown;
    private int lastMouseX;
    private int lastMouseY;

    private static final float PAN_SPEED = 30f;
    private static final float EDGE_SCROLL_SPEED = 400f;
    private static final float ZOOM_SPEED = 1f;
    private static final float ROTATE_SPEED = 0.005f;

    private float panLeft;
    private float panRight;
    private float panUp;
    private float panDown;
    private float edgePanLeft;
    private float edgePanRight;
    private float edgePanUp;
    private float edgePanDown;

    public RtsCameraInputListener(RtsCamera camera, int screenWidth, int screenHeight) {
        this.camera = camera;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    /**
     * Register all input mappings with JME InputManager.
     */
    public void registerWith(InputManager inputManager) {
        inputManager.addMapping("CAM_PAN_LEFT", new KeyTrigger(KeyInput.KEY_A),
                new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("CAM_PAN_RIGHT", new KeyTrigger(KeyInput.KEY_D),
                new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("CAM_PAN_UP", new KeyTrigger(KeyInput.KEY_W),
                new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("CAM_PAN_DOWN", new KeyTrigger(KeyInput.KEY_S),
                new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping("CAM_ZOOM_IN", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        inputManager.addMapping("CAM_ZOOM_OUT", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
        inputManager.addMapping("CAM_ROTATE_BUTTON", new MouseButtonTrigger(MouseInput.BUTTON_MIDDLE));

        inputManager.addListener(this,
                "CAM_PAN_LEFT", "CAM_PAN_RIGHT", "CAM_PAN_UP", "CAM_PAN_DOWN",
                "CAM_ZOOM_IN", "CAM_ZOOM_OUT", "CAM_ROTATE_BUTTON");

        log.info("RtsCameraInputListener registered");
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        switch (name) {
            case "CAM_PAN_LEFT" -> panLeft = value;
            case "CAM_PAN_RIGHT" -> panRight = value;
            case "CAM_PAN_UP" -> panUp = value;
            case "CAM_PAN_DOWN" -> panDown = value;
        }
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        switch (name) {
            case "CAM_ZOOM_IN" -> {
                if (isPressed) {
                    camera.zoom(-ZOOM_SPEED);
                }
            }
            case "CAM_ZOOM_OUT" -> {
                if (isPressed) {
                    camera.zoom(ZOOM_SPEED);
                }
            }
            case "CAM_ROTATE_BUTTON" -> middleButtonDown = isPressed;
        }
    }

    /**
     * Apply accumulated input each frame. Call from simpleUpdate.
     */
    public void update(float tpf, int mouseX, int mouseY) {
        float dt = Math.min(tpf, 0.1f);

        float dx = (panRight - panLeft) * PAN_SPEED * dt;
        float dz = (panUp - panDown) * PAN_SPEED * dt;

        if (Math.abs(dx) > 0.001f || Math.abs(dz) > 0.001f) {
            camera.pan(dx, dz);
        }

        if (mouseX <= camera.getScrollEdgeSize()) {
            camera.pan(-EDGE_SCROLL_SPEED * dt, 0);
        }
        if (mouseX >= screenWidth - camera.getScrollEdgeSize()) {
            camera.pan(EDGE_SCROLL_SPEED * dt, 0);
        }
        if (mouseY <= camera.getScrollEdgeSize()) {
            camera.pan(0, EDGE_SCROLL_SPEED * dt);
        }
        if (mouseY >= screenHeight - camera.getScrollEdgeSize()) {
            camera.pan(0, -EDGE_SCROLL_SPEED * dt);
        }

        if (middleButtonDown) {
            float rotateDelta = (mouseX - lastMouseX) * ROTATE_SPEED;
            if (Math.abs(rotateDelta) > 0.001f) {
                camera.rotate(-rotateDelta);
            }
        }

        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    private float cameraScrollEdgeSize = 10;

    public void setScrollEdgeSize(int size) {
        this.cameraScrollEdgeSize = size;
    }
}
