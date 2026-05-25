package com.jrts.movement;

import com.jrts.scene.FlatTerrainHeightProvider;
import com.jrts.scene.TerrainHeightProvider;
import com.jme3.math.Vector3f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SimpleLineNavigationTest {

    private NavigationService navigation;
    private TerrainHeightProvider terrain;

    @BeforeEach
    void setUp() {
        terrain = new FlatTerrainHeightProvider(0f, 100f);
        navigation = new SimpleLineNavigation(terrain);
    }

    @Test
    void returnsDestinationAsSingleWaypoint() {
        Vector3f start = new Vector3f(0, 0, 0);
        Vector3f end = new Vector3f(50, 0, 50);

        List<Vector3f> path = navigation.computePath(start, end);
        assertEquals(1, path.size());
        assertEquals(new Vector3f(50, 0, 50), path.get(0));
    }

    @Test
    void clampsDestinationToMapBounds() {
        Vector3f start = new Vector3f(0, 0, 0);
        Vector3f end = new Vector3f(200, 0, 0);

        List<Vector3f> path = navigation.computePath(start, end);
        assertEquals(1, path.size());
        assertEquals(100f, path.get(0).x, 0.001f);
    }

    @Test
    void pathWithinBoundsIsNotClamped() {
        Vector3f start = new Vector3f(0, 0, 0);
        Vector3f end = new Vector3f(50, 0, 30);

        List<Vector3f> path = navigation.computePath(start, end);
        assertEquals(end.x, path.get(0).x, 0.001f);
        assertEquals(end.z, path.get(0).z, 0.001f);
    }

    @Test
    void neverReturnsNull() {
        List<Vector3f> path = navigation.computePath(
                new Vector3f(0, 0, 0), new Vector3f(10, 0, 10));
        assertNotNull(path);
        assertFalse(path.isEmpty());
    }
}
