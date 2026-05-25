package com.jrts.scene;

import com.jme3.math.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stage 1 implementation — returns constant height for every point.
 * When Stage 2+ introduces PNG heightmaps, swap this class with
 * HeightmapTerrainProvider. No other system code changes.
 *
 * Map bounds are set at construction time.
 */
public class FlatTerrainHeightProvider implements TerrainHeightProvider {

    private static final Logger log = LoggerFactory.getLogger(FlatTerrainHeightProvider.class);

    private final float height;
    private final float mapSize;

    public FlatTerrainHeightProvider(float height, float mapSize) {
        this.height = height;
        this.mapSize = mapSize;
        log.info("Created FlatTerrainHeightProvider: height={}, mapSize={}", height, mapSize);
    }

    @Override
    public float getHeight(float worldX, float worldZ) {
        return height;
    }

    @Override
    public Vector3f getNormal(float worldX, float worldZ) {
        return Vector3f.UNIT_Y;
    }

    @Override
    public boolean isWater(float worldX, float worldZ) {
        return false;
    }

    @Override
    public float getGradient(float worldX, float worldZ) {
        return 0f;
    }

    @Override
    public boolean isInBounds(float worldX, float worldZ) {
        return Math.abs(worldX) <= mapSize && Math.abs(worldZ) <= mapSize;
    }

    @Override
    public float getMapMinX() {
        return -mapSize;
    }

    @Override
    public float getMapMaxX() {
        return mapSize;
    }

    @Override
    public float getMapMinZ() {
        return -mapSize;
    }

    @Override
    public float getMapMaxZ() {
        return mapSize;
    }

    @Override
    public float getWaterLevel() {
        return -Float.MAX_VALUE;
    }
}
