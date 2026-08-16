package com.jrts.pathfinding;

/**
 * Describes how a particular unit traverses the grid.
 *
 * @param surfaces       {@link SurfaceMask} bitmask of traversable surfaces
 * @param maxClimb       maximum height change per cell step (ramps); a larger step is a cliff
 * @param clearanceCells number of cells the unit's radius covers (footprint is (2c+1) x (2c+1))
 */
public record TraversalProfile(int surfaces, float maxClimb, int clearanceCells) {

    public static final TraversalProfile GROUND =
            new TraversalProfile(SurfaceMask.GROUND, 1f, 0);

    public static final TraversalProfile AMPHIBIOUS =
            new TraversalProfile(SurfaceMask.GROUND | SurfaceMask.WATER, 1f, 0);
}
