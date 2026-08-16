package com.jrts.pathfinding;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ObstacleTest {

    @Test
    void boxMarksExpectedCells() {
        Grid grid = new Grid(0f, 0f, 10, 10, 1f);
        Obstacle box = Obstacle.box(5f, 5f, 2f, 2f);

        List<GridCell> marked = box.markBlocked(grid, 0f);

        // Box [3,7] x [3,7]; cell centers (x+0.5, z+0.5) inside -> x,z in 3..6.
        assertEquals(16, marked.size());
        assertTrue(grid.isWalkable(new GridCell(2, 2)));
        assertFalse(grid.isWalkable(new GridCell(3, 3)));
        assertFalse(grid.isWalkable(new GridCell(6, 6)));
        assertTrue(grid.isWalkable(new GridCell(7, 7)));
    }

    @Test
    void rotatedBoxBlocksCellsInsideLocalFrame() {
        Grid grid = new Grid(0f, 0f, 20, 20, 1f);
        // A thin box rotated 90 degrees: extends along Z instead of X.
        Obstacle rotated = Obstacle.box(10f, 10f, 4f, 1f, (float) Math.PI / 2f);

        rotated.markBlocked(grid, 0f);

        assertFalse(grid.isWalkable(new GridCell(10, 8)), "long axis along Z should block");
        assertTrue(grid.isWalkable(new GridCell(14, 10)), "short axis along X stays narrow");
    }

    @Test
    void cylinderMarksCircularFootprint() {
        Grid grid = new Grid(0f, 0f, 10, 10, 1f);
        Obstacle cyl = Obstacle.cylinder(4.5f, 4.5f, 1.5f);

        cyl.markBlocked(grid, 0f);

        // Center cell (4,4) is inside radius; a far cell is not.
        assertFalse(grid.isWalkable(new GridCell(4, 4)));
        assertTrue(grid.isWalkable(new GridCell(0, 0)));
        assertTrue(grid.isWalkable(new GridCell(9, 9)));
    }

    @Test
    void wallAsBoxBlocksCellsAlongSegment() {
        Grid grid = new Grid(0f, 0f, 10, 10, 1f);
        // Segment from (4.5,1) to (4.5,8) with halfWidth 0.5 == box centered at (4.5,4.5)
        // with half extents (0.5, 3.5).
        Obstacle wall = Obstacle.box(4.5f, 4.5f, 0.5f, 3.5f);

        wall.markBlocked(grid, 0f);

        assertFalse(grid.isWalkable(new GridCell(4, 4)));
        assertTrue(grid.isWalkable(new GridCell(0, 4)));
        assertTrue(grid.isWalkable(new GridCell(8, 4)));
    }

    @Test
    void clearanceInflatesFootprint() {
        Grid grid = new Grid(0f, 0f, 10, 10, 1f);
        Obstacle cyl = Obstacle.cylinder(5f, 5f, 0.2f);

        cyl.markBlocked(grid, 2f);

        // radius 0.2 + clearance 2 -> cells within ~2.2 of (5,5) blocked.
        assertFalse(grid.isWalkable(new GridCell(5, 5)));
        assertFalse(grid.isWalkable(new GridCell(3, 5)));
        assertTrue(grid.isWalkable(new GridCell(1, 5)));
    }
}
