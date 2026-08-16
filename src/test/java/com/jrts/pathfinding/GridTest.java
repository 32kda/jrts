package com.jrts.pathfinding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GridTest {

    @Test
    void worldToCellFloorSemantics() {
        Grid grid = new Grid(0f, 0f, 10, 10, 1f);
        assertEquals(new GridCell(0, 0), grid.worldToCell(0.5f, 0.5f));
        assertEquals(new GridCell(3, 4), grid.worldToCell(3.9f, 4.2f));
        assertEquals(new GridCell(9, 9), grid.worldToCell(9.99f, 9.99f));
    }

    @Test
    void worldToCellHandlesNegativeCoordinates() {
        Grid grid = new Grid(-5f, -5f, 10, 10, 1f);
        assertEquals(new GridCell(0, 0), grid.worldToCell(-4.5f, -4.5f));
        assertEquals(new GridCell(1, 2), grid.worldToCell(-3.4f, -2.6f));
    }

    @Test
    void cellCenterIsMiddleOfCell() {
        Grid grid = new Grid(0f, 0f, 10, 10, 2f);
        assertEquals(3f, grid.cellCenterX(new GridCell(1, 0)), 0.001f);
        assertEquals(5f, grid.cellCenterZ(new GridCell(0, 2)), 0.001f);
    }

    @Test
    void fromBoundsComputesDimensions() {
        Grid grid = Grid.fromBounds(-10f, -10f, 10f, 10f, 1f);
        assertEquals(20, grid.getWidth());
        assertEquals(20, grid.getHeight());
        assertEquals(-10f, grid.getOriginX(), 0.001f);
    }

    @Test
    void cellsWalkableByDefault() {
        Grid grid = new Grid(0f, 0f, 5, 5, 1f);
        assertTrue(grid.isWalkable(new GridCell(2, 2)));
    }

    @Test
    void outOfBoundsIsBlocked() {
        Grid grid = new Grid(0f, 0f, 5, 5, 1f);
        assertFalse(grid.isWalkable(new GridCell(5, 5)));
        assertFalse(grid.isWalkable(new GridCell(-1, 0)));
    }

    @Test
    void setBlockedTogglesWalkability() {
        Grid grid = new Grid(0f, 0f, 5, 5, 1f);
        GridCell cell = new GridCell(2, 2);
        grid.setBlocked(cell, true);
        assertFalse(grid.isWalkable(cell));
        grid.setBlocked(cell, false);
        assertTrue(grid.isWalkable(cell));
    }

    @Test
    void isLineWalkableAcrossFreeLine() {
        Grid grid = new Grid(0f, 0f, 10, 10, 1f);
        assertTrue(grid.isLineWalkable(new GridCell(0, 0), new GridCell(9, 0)));
        assertTrue(grid.isLineWalkable(new GridCell(0, 0), new GridCell(9, 9)));
    }

    @Test
    void isLineWalkableBlockedByObstacle() {
        Grid grid = new Grid(0f, 0f, 10, 10, 1f);
        grid.setBlocked(new GridCell(5, 0), true);
        assertFalse(grid.isLineWalkable(new GridCell(0, 0), new GridCell(9, 0)));
    }

    @Test
    void typeHeightCostComponentAccessors() {
        Grid grid = new Grid(0f, 0f, 5, 5, 1f);
        GridCell cell = new GridCell(2, 3);
        assertEquals(CellType.CLEAR, grid.getType(cell));
        assertEquals(0f, grid.getHeight(cell), 0.001f);
        assertEquals(1f, grid.getCost(cell), 0.001f);
        assertEquals(-1, grid.getComponentId(cell));

        grid.setType(cell, CellType.WATER);
        grid.setHeight(cell, 7f);
        grid.setCost(cell, 2.5f);
        grid.setComponentId(cell, 42);

        assertEquals(CellType.WATER, grid.getType(cell));
        assertEquals(7f, grid.getHeight(cell), 0.001f);
        assertEquals(2.5f, grid.getCost(cell), 0.001f);
        assertEquals(42, grid.getComponentId(cell));
    }

    @Test
    void isTraversableRespectsSurfaceMask() {
        Grid grid = new Grid(0f, 0f, 5, 5, 1f);
        GridCell water = new GridCell(1, 1);
        grid.setType(water, CellType.WATER);

        assertTrue(grid.isTraversable(water, new TraversalProfile(SurfaceMask.WATER, 1f, 0)));
        assertFalse(grid.isTraversable(water, TraversalProfile.GROUND));
        assertTrue(grid.isTraversable(water, TraversalProfile.AMPHIBIOUS));
        assertFalse(grid.isTraversable(new GridCell(1, 0), new TraversalProfile(SurfaceMask.WATER, 1f, 0)));
    }

    @Test
    void hasClearanceChecksFootprint() {
        Grid grid = new Grid(0f, 0f, 5, 5, 1f);
        grid.setBlocked(new GridCell(3, 2), true);
        TraversalProfile one = new TraversalProfile(SurfaceMask.GROUND, 1f, 1);

        assertTrue(grid.hasClearance(new GridCell(1, 1), one));
        assertFalse(grid.hasClearance(new GridCell(2, 2), one), "footprint overlaps the obstacle");
    }

    @Test
    void defaultLayerIsZero() {
        assertEquals(0, new Grid(0f, 0f, 2, 2, 1f).getLayer());
        assertEquals(3, new Grid(0f, 0f, 2, 2, 1f, 3).getLayer());
    }
}
