package com.jrts.tools.preview;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

/**
 * Studio-style orbit camera used ONLY by the preview app.
 * Separate from RtsCamera — different use case (object inspection vs battlefield navigation).
 *
 * Controls:
 *   Left mouse drag → orbit (yaw + pitch)
 *   Scroll wheel    → zoom (distance)
 *   Right mouse drag → pan (move look-at point)
 */
public class OrbitCamera {

    private final Camera cam;
    private final Vector3f lookAt = new Vector3f(0, 0, 0);
    private float distance = 15f;
    private float yaw = 0f;
    private float pitch = 0.3f;

    private boolean leftDown;
    private boolean rightDown;
    private int lastMouseX;
    private int lastMouseY;

    private static final float ORBIT_SPEED = 0.005f;
    private static final float ZOOM_SPEED = 0.5f;
    private static final float PAN_SPEED = 0.01f;

    private float zoomDelta;

    public OrbitCamera(Camera cam) {
        this.cam = cam;
    }

    public void update(float tpf) {
        distance = Math.max(1f, Math.min(50f, distance + zoomDelta * ZOOM_SPEED));
        zoomDelta = 0;

        float camX = lookAt.x + distance * FastMath.cos(pitch) * FastMath.sin(yaw);
        float camY = lookAt.y + distance * FastMath.sin(pitch);
        float camZ = lookAt.z + distance * FastMath.cos(pitch) * FastMath.cos(yaw);

        cam.setLocation(new Vector3f(camX, camY, camZ));
        cam.lookAt(lookAt, Vector3f.UNIT_Y);
    }

    public void onMouseMove(int mouseX, int mouseY, boolean leftPressed, boolean rightPressed) {
        leftDown = leftPressed;
        rightDown = rightPressed;

        if (leftDown) {
            float dx = mouseX - lastMouseX;
            float dy = mouseY - lastMouseY;
            yaw -= dx * ORBIT_SPEED;
            pitch -= dy * ORBIT_SPEED;
            pitch = Math.max(-FastMath.HALF_PI + 0.01f,
                    Math.min(FastMath.HALF_PI - 0.01f, pitch));
        }

        if (rightDown) {
            float dx = mouseX - lastMouseX;
            float dy = mouseY - lastMouseY;
            float cosYaw = FastMath.cos(yaw);
            float sinYaw = FastMath.sin(yaw);
            lookAt.x += (-cosYaw * dx + sinYaw * dy) * PAN_SPEED;
            lookAt.z += (-sinYaw * dx - cosYaw * dy) * PAN_SPEED;
        }

        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    public void onScroll(float amount) {
        zoomDelta += amount;
    }

    public void setLookAt(Vector3f point) {
        lookAt.set(point);
    }

    public void reset() {
        lookAt.set(0, 0, 0);
        distance = 15f;
        yaw = 0f;
        pitch = 0.3f;
    }

    public float getDistance() {
        return distance;
    }
}
