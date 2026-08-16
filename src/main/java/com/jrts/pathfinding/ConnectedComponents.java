package com.jrts.pathfinding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;

/**
 * Labels connected components of traversable cells on a {@link Grid} for a given
 * {@link TraversalProfile}. Enables O(1) reachability rejection: if start and goal
 * are in different components, no path exists.
 *
 * Uses 4-connectivity, which matches the effective reachability of 8-direction A*
 * with corner-cut prevention (a diagonal move requires its two orthogonal neighbors
 * to be open, so any diagonal step can be replaced by two orthogonal steps).
 */
public class ConnectedComponents {

    private static final Logger log = LoggerFactory.getLogger(ConnectedComponents.class);

    /**
     * Recompute component ids for every cell traversable under the given profile.
     *
     * @return the number of components found
     */
    public int compute(Grid grid, TraversalProfile profile) {
        for (int z = 0; z < grid.getHeight(); z++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                grid.setComponentId(new GridCell(x, z), -1);
            }
        }

        int nextId = 0;
        for (int z = 0; z < grid.getHeight(); z++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                GridCell cell = new GridCell(x, z);
                if (!grid.isTraversable(cell, profile) || grid.getComponentId(cell) != -1) {
                    continue;
                }
                floodFill(grid, cell, profile, nextId++);
            }
        }
        log.debug("Computed {} connected components on {}x{} grid", nextId,
                grid.getWidth(), grid.getHeight());
        return nextId;
    }

    /**
     * @return true if two cells are in the same (previously computed) component
     */
    public static boolean sameComponent(Grid grid, GridCell a, GridCell b) {
        int ca = grid.getComponentId(a);
        int cb = grid.getComponentId(b);
        return ca != -1 && ca == cb;
    }

    private void floodFill(Grid grid, GridCell start, TraversalProfile profile, int id) {
        ArrayDeque<GridCell> stack = new ArrayDeque<>();
        stack.push(start);
        grid.setComponentId(start, id);

        int[] dx = {1, -1, 0, 0};
        int[] dz = {0, 0, 1, -1};

        while (!stack.isEmpty()) {
            GridCell c = stack.pop();
            for (int d = 0; d < 4; d++) {
                GridCell n = new GridCell(c.x() + dx[d], c.z() + dz[d]);
                if (!grid.isInBounds(n)) {
                    continue;
                }
                if (grid.getComponentId(n) != -1) {
                    continue;
                }
                if (!grid.isTraversable(n, profile)) {
                    continue;
                }
                grid.setComponentId(n, id);
                stack.push(n);
            }
        }
    }
}
