package com.jrts.pathfinding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConnectedComponentsTest {

    private final ConnectedComponents components = new ConnectedComponents();

    @Test
    void openGridIsSingleComponent() {
        Grid grid = new Grid(0f, 0f, 10, 10, 1f);
        int count = components.compute(grid, TraversalProfile.GROUND);
        assertEquals(1, count);
        assertTrue(ConnectedComponents.sameComponent(grid,
                new GridCell(0, 0), new GridCell(9, 9)));
    }

    @Test
    void wallSplitsGridIntoTwoComponents() {
        Grid grid = new Grid(0f, 0f, 5, 5, 1f);
        for (int z = 0; z < 5; z++) {
            grid.setBlocked(new GridCell(2, z), true);
        }
        int count = components.compute(grid, TraversalProfile.GROUND);
        assertEquals(2, count);
        assertFalse(ConnectedComponents.sameComponent(grid,
                new GridCell(0, 0), new GridCell(4, 4)));
        assertTrue(ConnectedComponents.sameComponent(grid,
                new GridCell(0, 0), new GridCell(1, 4)));
    }

    @Test
    void waterIsNotPartOfGroundComponent() {
        Grid grid = new Grid(0f, 0f, 5, 5, 1f);
        for (int x = 0; x < 5; x++) {
            grid.setType(new GridCell(x, 2), CellType.WATER);
        }
        components.compute(grid, TraversalProfile.GROUND);

        GridCell ground = new GridCell(0, 0);
        GridCell water = new GridCell(0, 2);
        assertEquals(-1, grid.getComponentId(water), "water is not in the ground component");
        assertTrue(grid.getComponentId(ground) >= 0);
        assertFalse(ConnectedComponents.sameComponent(grid, ground, water));
    }

    @Test
    void unreachableCellsHaveDifferentComponents() {
        Grid grid = new Grid(0f, 0f, 4, 4, 1f);
        // Fully enclose a single cell.
        grid.setBlocked(new GridCell(0, 1), true);
        grid.setBlocked(new GridCell(2, 1), true);
        grid.setBlocked(new GridCell(1, 0), true);
        grid.setBlocked(new GridCell(1, 2), true);
        components.compute(grid, TraversalProfile.GROUND);

        GridCell outside = new GridCell(3, 3);
        GridCell enclosed = new GridCell(1, 1);
        assertNotEquals(grid.getComponentId(outside), grid.getComponentId(enclosed));
    }
}
