package com.jrts.pathfinding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SurfaceMaskTest {

    @Test
    void fromCellTypeMapsToSurface() {
        assertEquals(SurfaceMask.GROUND, SurfaceMask.fromCellType(CellType.CLEAR));
        assertEquals(SurfaceMask.GROUND, SurfaceMask.fromCellType(CellType.RAMP));
        assertEquals(SurfaceMask.WATER, SurfaceMask.fromCellType(CellType.WATER));
        assertEquals(SurfaceMask.CLIFF, SurfaceMask.fromCellType(CellType.CLIFF));
        assertEquals(0, SurfaceMask.fromCellType(CellType.OBSTACLE));
        assertEquals(0, SurfaceMask.fromCellType(CellType.IMPASSABLE));
    }

    @Test
    void allowsRespectsProvidedSurface() {
        assertTrue(SurfaceMask.allows(SurfaceMask.GROUND, CellType.CLEAR));
        assertFalse(SurfaceMask.allows(SurfaceMask.GROUND, CellType.WATER));
        assertTrue(SurfaceMask.allows(SurfaceMask.WATER, CellType.WATER));
        assertTrue(SurfaceMask.allows(SurfaceMask.GROUND | SurfaceMask.WATER, CellType.WATER));
        assertFalse(SurfaceMask.allows(SurfaceMask.GROUND, CellType.OBSTACLE));
        assertFalse(SurfaceMask.allows(SurfaceMask.WATER, CellType.IMPASSABLE));
    }
}
