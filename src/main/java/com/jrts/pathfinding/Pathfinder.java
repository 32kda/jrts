package com.jrts.pathfinding;

/**
 * Finds a path between two cells on a {@link Grid} for a given {@link TraversalProfile}.
 *
 * Implementations: {@link AStarPathfinder} (fine-grained) and {@link HpaPathfinder}
 * (hierarchical, for large grids / many queries).
 */
public interface Pathfinder {

    /**
     * @return a path from start (inclusive) to goal (inclusive), or a failure result
     */
    PathResult findPath(Grid grid, GridCell start, GridCell goal, TraversalProfile profile);
}
