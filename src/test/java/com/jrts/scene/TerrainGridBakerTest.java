package com.jrts.scene;

import com.jrts.pathfinding.CellType;
import com.jrts.pathfinding.Grid;
import com.jrts.pathfinding.GridCell;
import com.jrts.pathfinding.Obstacle;
import com.jrts.pathfinding.TraversalProfile;
import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TerrainGridBakerTest {

    private final TerrainGridBaker baker = new TerrainGridBaker();

    @Test
    void bakeMarksObstacleCells() {
        TerrainHeightProvider terrain = new FlatTerrainHeightProvider(0f, 100f);
        Grid grid = baker.bake(terrain, 1f, List.of(Obstacle.box(0f, 0f, 5f, 5f)));

        GridCell blocked = grid.worldToCell(0f, 0f);
        GridCell clear = grid.worldToCell(50f, 50f);
        assertEquals(CellType.OBSTACLE, grid.getType(blocked));
        assertEquals(CellType.CLEAR, grid.getType(clear));
        assertFalse(grid.isTraversable(blocked, TraversalProfile.GROUND));
    }

    @Test
    void bakeMarksWaterCellsImpassableToGround() {
        FakeTerrain terrain = new FakeTerrain(100f);
        terrain.setWaterRegion(-10f, 10f, -10f, 10f);
        Grid grid = baker.bake(terrain, 1f, List.of());

        GridCell water = grid.worldToCell(0f, 0f);
        GridCell land = grid.worldToCell(50f, 50f);
        assertEquals(CellType.WATER, grid.getType(water));
        assertEquals(CellType.CLEAR, grid.getType(land));

        assertFalse(grid.isTraversable(water, TraversalProfile.GROUND));
        assertTrue(grid.isTraversable(water, TraversalProfile.AMPHIBIOUS));
    }

    @Test
    void bakeMarksCliffForSteepGradient() {
        FakeTerrain terrain = new FakeTerrain(100f);
        terrain.setSteepRegion(-10f, 10f, -10f, 10f);
        Grid grid = baker.bake(terrain, 1f, List.of());

        GridCell cliff = grid.worldToCell(0f, 0f);
        GridCell clear = grid.worldToCell(50f, 50f);
        assertEquals(CellType.CLIFF, grid.getType(cliff));
        assertEquals(CellType.CLEAR, grid.getType(clear));
        assertFalse(grid.isTraversable(cliff, TraversalProfile.GROUND));
    }

    @Test
    void bakeStoresHeights() {
        FakeTerrain terrain = new FakeTerrain(100f);
        terrain.setHeight(7.5f);
        Grid grid = baker.bake(terrain, 1f, List.of());
        assertEquals(7.5f, grid.getHeight(grid.worldToCell(10f, 10f)), 0.001f);
    }

    private static class FakeTerrain implements TerrainHeightProvider {
        private final float mapSize;
        private float height = 0f;
        private float wx0 = Float.NaN;
        private float wx1 = Float.NaN;
        private float wz0 = Float.NaN;
        private float wz1 = Float.NaN;
        private float sx0 = Float.NaN;
        private float sx1 = Float.NaN;
        private float sz0 = Float.NaN;
        private float sz1 = Float.NaN;

        FakeTerrain(float mapSize) {
            this.mapSize = mapSize;
        }

        void setHeight(float h) {
            this.height = h;
        }

        void setWaterRegion(float minX, float maxX, float minZ, float maxZ) {
            wx0 = minX; wx1 = maxX; wz0 = minZ; wz1 = maxZ;
        }

        void setSteepRegion(float minX, float maxX, float minZ, float maxZ) {
            sx0 = minX; sx1 = maxX; sz0 = minZ; sz1 = maxZ;
        }

        private boolean in(float x, float z, float x0, float x1, float z0, float z1) {
            return x >= x0 && x <= x1 && z >= z0 && z <= z1;
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
            return in(worldX, worldZ, wx0, wx1, wz0, wz1);
        }

        @Override
        public float getGradient(float worldX, float worldZ) {
            return in(worldX, worldZ, sx0, sx1, sz0, sz1) ? 100f : 0f;
        }

        @Override
        public boolean isInBounds(float worldX, float worldZ) {
            return Math.abs(worldX) <= mapSize && Math.abs(worldZ) <= mapSize;
        }

        @Override
        public float getMapMinX() { return -mapSize; }

        @Override
        public float getMapMaxX() { return mapSize; }

        @Override
        public float getMapMinZ() { return -mapSize; }

        @Override
        public float getMapMaxZ() { return mapSize; }

        @Override
        public float getWaterLevel() { return -Float.MAX_VALUE; }
    }
}
