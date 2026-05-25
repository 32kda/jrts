package com.jrts.scene;

import com.jme3.math.Vector3f;

/**
 * Single interface for all terrain queries.
 * All game systems depend on this interface, never on concrete terrain classes.
 * Thread-safe by contract (designed for future worker-thread pathfinding).
 */
public interface TerrainHeightProvider {

    /**
     * @param worldX world-space X coordinate
     * @param worldZ world-space Z coordinate
     * @return terrain Y (height) at that point
     */
    float getHeight(float worldX, float worldZ);

    /**
     * @param worldX world-space X coordinate
     * @param worldZ world-space Z coordinate
     * @return surface normal vector at that point (unit length)
     */
    Vector3f getNormal(float worldX, float worldZ);

    /**
     * @return true if this point is water (height less than or equal to waterLevel)
     */
    boolean isWater(float worldX, float worldZ);

    /**
     * @return gradient magnitude (0 = flat, higher = steeper)
     */
    float getGradient(float worldX, float worldZ);

    /**
     * @return true if (x,z) is within map bounds
     */
    boolean isInBounds(float worldX, float worldZ);

    float getMapMinX();

    float getMapMaxX();

    float getMapMinZ();

    float getMapMaxZ();

    float getWaterLevel();
}
