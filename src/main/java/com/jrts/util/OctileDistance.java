package com.jrts.util;

/**
 * Octile distance heuristic helper for grid-based pathfinding.
 * Octile distance is a combination of Chebyshev and Manhattan distances,
 * suitable for grids with 8-directional movement.
 * Stub for Stage 1 — used when real A* arrives in Stage 2+.
 */
public final class OctileDistance {

    private OctileDistance() {
    }

    public static final float SQRT2 = 1.41421356237f;

    /**
     * Computes octile distance between two grid cells.
     *
     * @param x1 start cell X
     * @param y1 start cell Y
     * @param x2 end cell X
     * @param y2 end cell Y
     * @return octile distance in grid cells
     */
    public static float distance(int x1, int y1, int x2, int y2) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int min = Math.min(dx, dy);
        int max = Math.max(dx, dy);
        return SQRT2 * min + (max - min);
    }

    /**
     * Computes octile distance between two world positions.
     *
     * @param x1 start world X
     * @param z1 start world Z
     * @param x2 end world X
     * @param z2 end world Z
     * @param cellSize size of a grid cell in world units
     * @return octile distance in world units
     */
    public static float distanceWorld(float x1, float z1, float x2, float z2, float cellSize) {
        int cx1 = (int) (x1 / cellSize);
        int cz1 = (int) (z1 / cellSize);
        int cx2 = (int) (x2 / cellSize);
        int cz2 = (int) (z2 / cellSize);
        return distance(cx1, cz1, cx2, cz2) * cellSize;
    }
}
