package com.jrts.pathfinding;

/**
 * Terrain classification of a pathfinding cell. The search algorithms treat every
 * non-walkable type (water, cliff, obstacle, impassable) identically regardless of
 * source, so a blocked cell is a blocked cell whether it came from an obstacle,
 * a body of water, or a cliff.
 */
public enum CellType {
    /** Free, walkable ground. */
    CLEAR,
    /** Water — walkable only to amphibious/hover units. */
    WATER,
    /** Cliff / steep slope — walkable only to cliff-climbing units (reserved). */
    CLIFF,
    /** A gentle height transition between levels. */
    RAMP,
    /** Occupied by a static obstacle (building, rock, wall). */
    OBSTACLE,
    /** Permanently impassable (map border, void). */
    IMPASSABLE
}
