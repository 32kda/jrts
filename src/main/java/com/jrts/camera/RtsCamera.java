package com.jrts.camera;

import com.jrts.scene.TerrainHeightProvider;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tactical RTS camera wrapping JME's Camera.
 *
 * Coordinate convention:
 *   JME world X → East (right on screen)
 *   JME world Y → Up (height above terrain)
 *   JME world Z → South (up on screen in default view)
 *
 * The camera orbits a fixed pivot point at ground level.
 * Camera position is derived from pivot + spherical offsets:
 *   pitch: angle above horizon (0 = edge-on, PI/2 = top-down)
 *   yaw: compass rotation around Y axis
 *   distance: zoom level (height above pivot)
 */
public class RtsCamera {

    private static final Logger log = LoggerFactory.getLogger(RtsCamera.class);

    private final Camera cam;
    private final Vector3f pivot = new Vector3f(0, 0, 0);

    private float pitch;
    private float yaw;
    private float distance;

    private TerrainHeightProvider terrain;
    private boolean boundsEnabled = true;

    private static final float MIN_DISTANCE = 20f;
    private static final float MAX_DISTANCE = 500f;
    private static final float MIN_PITCH = 0.1f;
    private static final float MAX_PITCH = 1.4f;
    private static final float DEFAULT_PITCH = 0.96f;
    private static final float DEFAULT_DISTANCE = 200f;

    private float panSpeed = 0.5f;
    private float zoomSpeed = 10f;
    private float rotateSpeed = 1.0f;
    private int scrollEdgeSize = 10;

    public RtsCamera(Camera cam) {
        this.cam = cam;
        this.pitch = DEFAULT_PITCH;
        this.yaw = 0f;
        this.distance = DEFAULT_DISTANCE;
        log.info("RtsCamera initialized: pivot={}, pitch={}, yaw={}, distance={}",
                pivot, pitch, yaw, distance);
    }

    /**
     * Recalculates cam location and look direction from pivot + pitch + yaw + distance.
     * Called every frame after input modifies any parameter.
     */
    public void update(float tpf) {
        float camX = pivot.x + distance * FastMath.cos(pitch) * FastMath.sin(yaw);
        float camY = pivot.y + distance * FastMath.sin(pitch);
        float camZ = pivot.z + distance * FastMath.cos(pitch) * FastMath.cos(yaw);

        cam.setLocation(new Vector3f(camX, camY, camZ));
        cam.lookAt(pivot, Vector3f.UNIT_Y);
    }

    public void pan(float dx, float dy) {
        float cosYaw = FastMath.cos(yaw);
        float sinYaw = FastMath.sin(yaw);
        float scale = panSpeed * (distance / 50f);

        pivot.x += (-cosYaw * dx - sinYaw * dy) * scale;
        pivot.z += (-sinYaw * dx + cosYaw * dy) * scale;

        if (boundsEnabled && terrain != null) {
            pivot.x = clamp(pivot.x, terrain.getMapMinX(), terrain.getMapMaxX());
            pivot.z = clamp(pivot.z, terrain.getMapMinZ(), terrain.getMapMaxZ());
        }
    }

    public void zoom(float amount) {
        distance = Math.max(MIN_DISTANCE, Math.min(MAX_DISTANCE, distance - amount * zoomSpeed));
    }

    public void rotate(float amount) {
        yaw += amount * rotateSpeed;
        yaw = yaw % FastMath.TWO_PI;
    }

    public boolean isMouseAtEdge(int mouseX, int mouseY, int screenW, int screenH) {
        return mouseX <= scrollEdgeSize
                || mouseX >= screenW - scrollEdgeSize
                || mouseY <= scrollEdgeSize
                || mouseY >= screenH - scrollEdgeSize;
    }

    public Vector3f getPivot() {
        return pivot.clone();
    }

    public void setPivot(Vector3f pivot) {
        this.pivot.set(pivot);
        if (boundsEnabled && terrain != null) {
            this.pivot.x = clamp(this.pivot.x, terrain.getMapMinX(), terrain.getMapMaxX());
            this.pivot.z = clamp(this.pivot.z, terrain.getMapMinZ(), terrain.getMapMaxZ());
        }
    }

    public float getPitch() {
        return pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public float getDistance() {
        return distance;
    }

    public void setPanSpeed(float panSpeed) {
        this.panSpeed = panSpeed;
    }

    public void setZoomSpeed(float zoomSpeed) {
        this.zoomSpeed = zoomSpeed;
    }

    public void setRotateSpeed(float rotateSpeed) {
        this.rotateSpeed = rotateSpeed;
    }

    public void setScrollEdgeSize(int scrollEdgeSize) {
        this.scrollEdgeSize = scrollEdgeSize;
    }

    public int getScrollEdgeSize() {
        return scrollEdgeSize;
    }

    public void setTerrainProvider(TerrainHeightProvider terrain) {
        this.terrain = terrain;
    }

    public void setBoundsEnabled(boolean enabled) {
        this.boundsEnabled = enabled;
    }

    public boolean isBoundsEnabled() {
        return boundsEnabled;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
