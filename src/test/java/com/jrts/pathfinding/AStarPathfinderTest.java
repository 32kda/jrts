package com.jrts.pathfinding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.jrts.pathfinding.PathfindingTestSupport.GridMap;
import static com.jrts.pathfinding.PathfindingTestSupport.parseGrid;
import static org.junit.jupiter.api.Assertions.*;

class AStarPathfinderTest {

    private AStarPathfinder pathfinder;

    @BeforeEach
    void setUp() {
        pathfinder = new AStarPathfinder();
    }

    @Test
    void straightPathAcrossOpenGrid() {
        GridMap map = parseGrid(".....", "S...G", ".....");
        PathResult result = find(map);

        assertTrue(result.found());
        assertEquals(map.start(), result.cells().get(0));
        assertEquals(map.goal(), result.cells().get(result.cells().size() - 1));
        for (GridCell c : result.cells()) {
            assertTrue(map.grid().isWalkable(c));
        }
    }

    @Test
    void pathDetoursAroundObstacle() {
        GridMap map = parseGrid("#####", "#S#G#", "#...#", "#####");
        PathResult result = find(map);

        assertTrue(result.found());
        assertEquals(map.start(), result.cells().get(0));
        assertEquals(map.goal(), result.cells().get(result.cells().size() - 1));
        assertEquals(5, result.cells().size());
        for (GridCell c : result.cells()) {
            assertTrue(map.grid().isWalkable(c));
        }
    }

    @Test
    void unreachableGoalFailsCleanly() {
        GridMap map = parseGrid("#####", "#S#G#", "#####");
        PathResult result = find(map);

        assertFalse(result.found());
        assertEquals(PathResult.UNREACHABLE, result.reason());
    }

    @Test
    void startEqualsGoalReturnsSingleCell() {
        GridMap map = parseGrid("...", ".S.", "...");
        PathResult result = pathfinder.findPath(map.grid(), map.start(), map.start(), TraversalProfile.GROUND);

        assertTrue(result.found());
        assertEquals(List.of(map.start()), result.cells());
    }

    @Test
    void blockedStartFailsWithReason() {
        GridMap map = parseGrid("...", "S.G", "...");
        map.grid().setBlocked(map.start(), true);
        PathResult result = find(map);

        assertFalse(result.found());
        assertEquals(PathResult.START_BLOCKED, result.reason());
    }

    @Test
    void blockedGoalFailsWithReason() {
        GridMap map = parseGrid("...", "S.G", "...");
        map.grid().setBlocked(map.goal(), true);
        PathResult result = find(map);

        assertFalse(result.found());
        assertEquals(PathResult.GOAL_BLOCKED, result.reason());
    }

    @Test
    void diagonalDoesNotCutCorner() {
        // S=(0,1), G=(1,0). The diagonal S->G would need (1,1) which is blocked.
        GridMap map = parseGrid("...", "S#.", ".G.");
        PathResult result = find(map);

        assertTrue(result.found());
        assertEquals(3, result.cells().size());
        assertFalse(result.cells().contains(new GridCell(1, 1)));
    }

    @Test
    void deterministicForRepeatedQueries() {
        GridMap map = parseGrid("#####", "#S#G#", "#...#", "#####");
        PathResult r1 = find(map);
        PathResult r2 = find(map);

        assertEquals(r1.cells(), r2.cells());
    }

    @Test
    void clearanceBlocksNarrowGap() {
        Grid grid = new Grid(0f, 0f, 5, 5, 1f);
        for (int x = 0; x < 5; x++) {
            if (x != 2) {
                grid.setType(new GridCell(x, 2), CellType.OBSTACLE);
            }
        }
        GridCell start = new GridCell(2, 0);
        GridCell goal = new GridCell(2, 4);

        assertTrue(pathfinder.findPath(grid, start, goal,
                new TraversalProfile(SurfaceMask.GROUND, 1f, 0)).found());
        assertFalse(pathfinder.findPath(grid, start, goal,
                new TraversalProfile(SurfaceMask.GROUND, 1f, 1)).found());
    }

    @Test
    void slopeBlocksClimbAboveMaxClimb() {
        Grid grid = new Grid(0f, 0f, 2, 1, 1f);
        grid.setHeight(new GridCell(1, 0), 1f);
        GridCell start = new GridCell(0, 0);
        GridCell goal = new GridCell(1, 0);

        assertTrue(pathfinder.findPath(grid, start, goal,
                new TraversalProfile(SurfaceMask.GROUND, 1f, 0)).found());
        assertFalse(pathfinder.findPath(grid, start, goal,
                new TraversalProfile(SurfaceMask.GROUND, 0.5f, 0)).found());
    }

    @Test
    void waterIsImpassableToGroundButNotAmphibious() {
        Grid grid = new Grid(0f, 0f, 5, 5, 1f);
        for (int x = 0; x < 5; x++) {
            grid.setType(new GridCell(x, 2), CellType.WATER);
        }
        GridCell start = new GridCell(2, 0);
        GridCell goal = new GridCell(2, 4);

        assertFalse(pathfinder.findPath(grid, start, goal, TraversalProfile.GROUND).found());
        assertTrue(pathfinder.findPath(grid, start, goal, TraversalProfile.AMPHIBIOUS).found());
    }

    private PathResult find(GridMap map) {
        return pathfinder.findPath(map.grid(), map.start(), map.goal(), TraversalProfile.GROUND);
    }
}
