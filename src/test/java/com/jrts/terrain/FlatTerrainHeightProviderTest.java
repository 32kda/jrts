package com.jrts.scene;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlatTerrainHeightProviderTest {

    private final FlatTerrainHeightProvider terrain = new FlatTerrainHeightProvider(0f, 100f);

    @Test
    void getHeightReturnsConstant() {
        assertEquals(0f, terrain.getHeight(0, 0), 0.001f);
        assertEquals(0f, terrain.getHeight(50, 50), 0.001f);
        assertEquals(0f, terrain.getHeight(-50, -50), 0.001f);
    }

    @Test
    void getNormalAlwaysPointsUp() {
        var normal = terrain.getNormal(0, 0);
        assertEquals(0f, normal.x, 0.001f);
        assertEquals(1f, normal.y, 0.001f);
        assertEquals(0f, normal.z, 0.001f);
        assertEquals(1f, normal.length(), 0.001f);
    }

    @Test
    void isWaterAlwaysFalse() {
        assertFalse(terrain.isWater(0, 0));
        assertFalse(terrain.isWater(100, 100));
    }

    @Test
    void getGradientAlwaysZero() {
        assertEquals(0f, terrain.getGradient(0, 0));
    }

    @Test
    void isInBoundsWithinRange() {
        assertTrue(terrain.isInBounds(0, 0));
        assertTrue(terrain.isInBounds(100, 100));
        assertTrue(terrain.isInBounds(-100, -100));
    }

    @Test
    void isInBoundsOutOfRange() {
        assertFalse(terrain.isInBounds(101, 0));
        assertFalse(terrain.isInBounds(0, 101));
        assertFalse(terrain.isInBounds(-101, 0));
        assertFalse(terrain.isInBounds(0, -101));
    }

    @Test
    void mapBoundsAreCorrect() {
        assertEquals(-100f, terrain.getMapMinX());
        assertEquals(100f, terrain.getMapMaxX());
        assertEquals(-100f, terrain.getMapMinZ());
        assertEquals(100f, terrain.getMapMaxZ());
    }

    @Test
    void waterLevelIsNegativeMax() {
        assertTrue(terrain.getWaterLevel() < -Float.MAX_VALUE / 2);
    }

    @Test
    void customHeightWorks() {
        FlatTerrainHeightProvider custom = new FlatTerrainHeightProvider(42f, 50f);
        assertEquals(42f, custom.getHeight(10, 20), 0.001f);
        assertTrue(custom.isInBounds(0, 0));
        assertFalse(custom.isInBounds(51, 0));
    }
}
