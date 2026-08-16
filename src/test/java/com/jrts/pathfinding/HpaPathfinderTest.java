package com.jrts.pathfinding;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HpaPathfinderTest {

    private static final TraversalProfile PROFILE = TraversalProfile.GROUND;

    private final HpaPathfinder hpa = new HpaPathfinder(3);
    private final AStarPathfinder fine = new AStarPathfinder();

    @Test
    void matchesFineAStarOnOpenGrid() {
        Grid grid = new Grid(0f, 0f, 20, 20, 1f);
        GridCell start = new GridCell(1, 1);
        GridCell goal = new GridCell(18, 18);

        assertEquals(pathCost(fine.findPath(grid, start, goal, PROFILE)),
                pathCost(hpa.findPath(grid, start, goal, PROFILE)), 0.001f);
    }

    @Test
    void matchesFineAStarAroundWall() {
        Grid grid = wallWithGap(20, 10, 5);
        GridCell start = new GridCell(1, 10);
        GridCell goal = new GridCell(18, 10);

        assertEquals(pathCost(fine.findPath(grid, start, goal, PROFILE)),
                pathCost(hpa.findPath(grid, start, goal, PROFILE)), 0.001f);
    }

    @Test
    void pathIsConnectedAndClear() {
        Grid grid = wallWithGap(20, 10, 5);
        GridCell start = new GridCell(1, 10);
        GridCell goal = new GridCell(18, 10);

        PathResult result = hpa.findPath(grid, start, goal, PROFILE);

        assertTrue(result.found());
        assertEquals(start, result.cells().get(0));
        assertEquals(goal, result.cells().get(result.cells().size() - 1));
        for (int i = 1; i < result.cells().size(); i++) {
            GridCell a = result.cells().get(i - 1);
            GridCell b = result.cells().get(i);
            assertTrue(Math.abs(a.x() - b.x()) <= 1 && Math.abs(a.z() - b.z()) <= 1,
                    "consecutive path cells must be adjacent");
        }
        for (GridCell c : result.cells()) {
            assertTrue(grid.isWalkable(c), "path must not enter blocked cells");
        }
    }

    @Test
    void unreachableGoalFails() {
        Grid grid = new Grid(0f, 0f, 20, 20, 1f);
        for (int z = 0; z < 20; z++) {
            grid.setBlocked(new GridCell(10, z), true);
        }
        PathResult result = hpa.findPath(grid, new GridCell(1, 10), new GridCell(18, 10), PROFILE);
        assertFalse(result.found());
        assertEquals(PathResult.UNREACHABLE, result.reason());
    }

    @Test
    void blockedStartFails() {
        Grid grid = new Grid(0f, 0f, 10, 10, 1f);
        GridCell start = new GridCell(1, 1);
        grid.setBlocked(start, true);
        PathResult result = hpa.findPath(grid, start, new GridCell(8, 8), PROFILE);
        assertFalse(result.found());
        assertEquals(PathResult.START_BLOCKED, result.reason());
    }

    @Test
    void blockedGoalFails() {
        Grid grid = new Grid(0f, 0f, 10, 10, 1f);
        GridCell goal = new GridCell(8, 8);
        grid.setBlocked(goal, true);
        PathResult result = hpa.findPath(grid, new GridCell(1, 1), goal, PROFILE);
        assertFalse(result.found());
        assertEquals(PathResult.GOAL_BLOCKED, result.reason());
    }

    @Test
    void startEqualsGoalReturnsSingleCell() {
        Grid grid = new Grid(0f, 0f, 10, 10, 1f);
        GridCell cell = new GridCell(5, 5);
        PathResult result = hpa.findPath(grid, cell, cell, PROFILE);
        assertTrue(result.found());
        assertEquals(List.of(cell), result.cells());
    }

    @Test
    void sameClusterUsesDirectPath() {
        Grid grid = new Grid(0f, 0f, 20, 20, 1f);
        // Both cells within the first 3x3 cluster.
        GridCell start = new GridCell(0, 0);
        GridCell goal = new GridCell(2, 2);
        PathResult result = hpa.findPath(grid, start, goal, PROFILE);
        assertTrue(result.found());
        assertEquals(pathCost(fine.findPath(grid, start, goal, PROFILE)),
                pathCost(result), 0.001f);
    }

    @Test
    void deterministicForRepeatedQueries() {
        Grid grid = wallWithGap(20, 10, 5);
        GridCell start = new GridCell(1, 10);
        GridCell goal = new GridCell(18, 10);
        assertEquals(hpa.findPath(grid, start, goal, PROFILE).cells(),
                hpa.findPath(grid, start, goal, PROFILE).cells());
    }

    private Grid wallWithGap(int size, int wallX, int gapZ) {
        Grid grid = new Grid(0f, 0f, size, size, 1f);
        for (int z = 0; z < size; z++) {
            if (z != gapZ) {
                grid.setBlocked(new GridCell(wallX, z), true);
            }
        }
        return grid;
    }

    private float pathCost(PathResult result) {
        return pathCost(result.cells());
    }

    private float pathCost(List<GridCell> cells) {
        float cost = 0f;
        for (int i = 1; i < cells.size(); i++) {
            GridCell a = cells.get(i - 1);
            GridCell b = cells.get(i);
            cost += (a.x() != b.x() && a.z() != b.z()) ? 14f : 10f;
        }
        return cost;
    }
}
