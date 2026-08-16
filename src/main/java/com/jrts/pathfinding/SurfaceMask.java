package com.jrts.pathfinding;

/**
 * Bitmask of movement surfaces a cell can provide and a unit can traverse.
 * Matches the {@code LocomotorTemplate.surfaces} convention (ground=1, water=2, ...).
 */
public final class SurfaceMask {

    public static final int GROUND = 1;
    public static final int WATER = 2;
    public static final int CLIFF = 4;
    public static final int AIR = 8;
    /** Reserved for future rubble terrain. */
    public static final int RUBBLE = 16;

    private SurfaceMask() {
    }

    /**
     * @return the surface provided by the given cell type, or 0 if the type is
     *         not traversable by anything (obstacle, impassable)
     */
    public static int fromCellType(CellType type) {
        return switch (type) {
            case CLEAR, RAMP -> GROUND;
            case WATER -> WATER;
            case CLIFF -> CLIFF;
            case OBSTACLE, IMPASSABLE -> 0;
        };
    }

    /**
     * @return true if a unit with the given surface mask may occupy a cell of the given type
     */
    public static boolean allows(int surfaces, CellType type) {
        int provided = fromCellType(type);
        return provided != 0 && (surfaces & provided) != 0;
    }
}
