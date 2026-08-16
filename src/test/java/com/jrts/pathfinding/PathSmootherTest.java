package com.jrts.pathfinding;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PathSmootherTest {

    private final PathSmoother smoother = new PathSmoother();

    @Test
    void straightLineCollapsesToEndpoints() {
        Grid grid = new Grid(0f, 0f, 10, 10, 1f);
        List<GridCell> path = List.of(
                new GridCell(0, 0), new GridCell(1, 0), new GridCell(2, 0),
                new GridCell(3, 0), new GridCell(4, 0));

        List<GridCell> smoothed = smoother.smooth(grid, path, TraversalProfile.GROUND);

        assertEquals(2, smoothed.size());
        assertEquals(path.get(0), smoothed.get(0));
        assertEquals(path.get(path.size() - 1), smoothed.get(smoothed.size() - 1));
    }

    @Test
    void lShapedPathKeepsCorner() {
        Grid grid = new Grid(0f, 0f, 10, 10, 1f);
        // The direct diagonal (0,0)->(2,2) is blocked by (1,1), so the corner must stay.
        grid.setBlocked(new GridCell(1, 1), true);
        List<GridCell> path = List.of(
                new GridCell(0, 0), new GridCell(0, 2), new GridCell(2, 2));

        List<GridCell> smoothed = smoother.smooth(grid, path, TraversalProfile.GROUND);

        assertEquals(3, smoothed.size());
        assertEquals(path, smoothed);
    }

    @Test
    void emptyAndSingleElementPaths() {
        Grid grid = new Grid(0f, 0f, 10, 10, 1f);
        assertTrue(smoother.smooth(grid, List.of(), TraversalProfile.GROUND).isEmpty());
        assertEquals(1, smoother.smooth(grid, List.of(new GridCell(1, 1)),
                TraversalProfile.GROUND).size());
    }

    @Test
    void preservesObstacleAvoidance() {
        Grid grid = new Grid(0f, 0f, 10, 10, 1f);
        grid.setBlocked(new GridCell(5, 5), true);
        List<GridCell> path = new ArrayList<>(List.of(
                new GridCell(0, 0), new GridCell(5, 0), new GridCell(9, 0)));

        List<GridCell> smoothed = smoother.smooth(grid, path, TraversalProfile.GROUND);
        for (int i = 0; i < smoothed.size() - 1; i++) {
            assertTrue(grid.isLineWalkable(smoothed.get(i), smoothed.get(i + 1)));
        }
    }
}
