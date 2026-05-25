package com.jrts.camera;

import com.jrts.scene.TerrainHeightProvider;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

/**
 * Encapsulates all screen ↔ world ↔ terrain coordinate conversions.
 * Stateless helper — no dependencies.
 *
 * All terrain queries go through TerrainHeightProvider, never a mesh.
 * This keeps the collision and rendering models completely separate.
 */
public final class ScreenMap {

    private ScreenMap() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Screen pixel → terrain world position.
     * Returns the terrain intersection point with the camera ray.
     * Uses terrainProvider.getHeight() for the Y value.
     *
     * @param screenX screen pixel X
     * @param screenY screen pixel Y
     * @param cam     current camera state
     * @param terrain height provider for ground Y
     * @return world position on terrain surface, or null if ray misses
     */
    public static Vector3f screenToTerrain(float screenX, float screenY, Camera cam,
                                           TerrainHeightProvider terrain) {
        Vector3f origin = cam.getWorldCoordinates(new Vector2f(screenX, screenY), 0f);
        Vector3f direction = cam.getWorldCoordinates(new Vector2f(screenX, screenY), 1f)
                .subtractLocal(origin).normalizeLocal();

        if (direction.y >= 0) {
            return null;
        }

        float t = -origin.y / direction.y;
        float wx = origin.x + t * direction.x;
        float wz = origin.z + t * direction.z;

        if (!terrain.isInBounds(wx, wz)) {
            return null;
        }

        return new Vector3f(wx, terrain.getHeight(wx, wz), wz);
    }

    /**
     * World position → screen pixel.
     */
    public static Vector2f worldToScreen(Vector3f worldPos, Camera cam, int screenW, int screenH) {
        Vector3f screenCoord = cam.getScreenCoordinates(worldPos);
        return new Vector2f(screenCoord.x, screenCoord.y);
    }

    /**
     * Clamp a world position to stay within map bounds (uses terrain provider).
     */
    public static Vector3f clampToMap(Vector3f pos, TerrainHeightProvider terrain) {
        float x = Math.max(terrain.getMapMinX(), Math.min(terrain.getMapMaxX(), pos.x));
        float z = Math.max(terrain.getMapMinZ(), Math.min(terrain.getMapMaxZ(), pos.z));
        return new Vector3f(x, terrain.getHeight(x, z), z);
    }
}
