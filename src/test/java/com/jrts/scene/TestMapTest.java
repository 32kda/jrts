package com.jrts.scene;

import com.jrts.pathfinding.Grid;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestMapTest {

    @Test
    void providesTerrainGridAndObstacles() {
        TestMap map = new TestMap();
        assertNotNull(map.terrain());
        assertNotNull(map.grid());
        assertEquals(3, map.obstacles().size());
    }

    @Test
    void obstaclesAreBakedIntoGrid() {
        TestMap map = new TestMap();
        Grid grid = map.grid();

        assertFalse(grid.isWalkable(grid.worldToCell(30f, 0f)),
                "cube obstacle cell should be blocked");
        assertFalse(grid.isWalkable(grid.worldToCell(-20f, 20f)),
                "cylinder obstacle cell should be blocked");
        assertTrue(grid.isWalkable(grid.worldToCell(-40f, -40f)),
                "open area should be walkable");
    }
}
