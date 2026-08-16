package com.jrts.pathfinding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A single layer of the pathfinding grid over the world XZ plane.
 *
 * Engine-independent: works only with floats and grid coordinates, no JME types.
 * Origin is the world coordinate of the lower corner of cell (0,0); cells grow
 * toward +X and +Z. Out-of-bounds cells are treated as impassable.
 *
 * Each cell carries:
 * <ul>
 *   <li>{@code type} — terrain classification ({@link CellType})</li>
 *   <li>{@code height} — terrain Y at the cell center</li>
 *   <li>{@code cost} — movement cost multiplier (&ge; 1)</li>
 *   <li>{@code componentId} — connected-component id for reachability (filled by {@link ConnectedComponents})</li>
 * </ul>
 *
 * {@code layer} is 0 for the ground; future bridges add stacked grids (1..n) with connect cells.
 */
public class Grid {

    private static final Logger log = LoggerFactory.getLogger(Grid.class);

    private final float originX;
    private final float originZ;
    private final int width;
    private final int height;
    private final float cellSize;
    private final int layer;

    private final CellType[] type;
    private final float[] heights;
    private final float[] cost;
    private final int[] componentId;

    public Grid(float originX, float originZ, int width, int height, float cellSize) {
        this(originX, originZ, width, height, cellSize, 0);
    }

    public Grid(float originX, float originZ, int width, int height, float cellSize, int layer) {
        this.originX = originX;
        this.originZ = originZ;
        this.width = width;
        this.height = height;
        this.cellSize = cellSize;
        this.layer = layer;
        int size = width * height;
        this.type = new CellType[size];
        java.util.Arrays.fill(this.type, CellType.CLEAR);
        this.heights = new float[size];
        this.cost = new float[size];
        java.util.Arrays.fill(this.cost, 1f);
        this.componentId = new int[size];
        java.util.Arrays.fill(this.componentId, -1);
        log.debug("Created grid {}x{} cells, cellSize={}, layer={}, origin=({},{})",
                width, height, cellSize, layer, originX, originZ);
    }

    /**
     * Build a ground-layer grid covering the given world bounds.
     */
    public static Grid fromBounds(float minX, float minZ, float maxX, float maxZ, float cellSize) {
        int width = (int) Math.ceil((maxX - minX) / cellSize);
        int height = (int) Math.ceil((maxZ - minZ) / cellSize);
        return new Grid(minX, minZ, width, height, cellSize, 0);
    }

    public float getCellSize() {
        return cellSize;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getLayer() {
        return layer;
    }

    public float getOriginX() {
        return originX;
    }

    public float getOriginZ() {
        return originZ;
    }

    public GridCell worldToCell(float worldX, float worldZ) {
        int x = (int) Math.floor((worldX - originX) / cellSize);
        int z = (int) Math.floor((worldZ - originZ) / cellSize);
        return new GridCell(x, z);
    }

    public float cellCenterX(GridCell cell) {
        return originX + (cell.x() + 0.5f) * cellSize;
    }

    public float cellCenterZ(GridCell cell) {
        return originZ + (cell.z() + 0.5f) * cellSize;
    }

    public boolean isInBounds(GridCell cell) {
        return cell.x() >= 0 && cell.x() < width && cell.z() >= 0 && cell.z() < height;
    }

    // --- Cell metadata accessors ---

    public CellType getType(GridCell cell) {
        if (!isInBounds(cell)) {
            return CellType.IMPASSABLE;
        }
        return type[cell.x() + cell.z() * width];
    }

    public void setType(GridCell cell, CellType value) {
        if (isInBounds(cell)) {
            type[cell.x() + cell.z() * width] = value;
        }
    }

    public float getHeight(GridCell cell) {
        if (!isInBounds(cell)) {
            return 0f;
        }
        return heights[cell.x() + cell.z() * width];
    }

    public void setHeight(GridCell cell, float value) {
        if (isInBounds(cell)) {
            heights[cell.x() + cell.z() * width] = value;
        }
    }

    public float getCost(GridCell cell) {
        if (!isInBounds(cell)) {
            return 1f;
        }
        return cost[cell.x() + cell.z() * width];
    }

    public void setCost(GridCell cell, float value) {
        if (isInBounds(cell)) {
            cost[cell.x() + cell.z() * width] = value;
        }
    }

    public int getComponentId(GridCell cell) {
        if (!isInBounds(cell)) {
            return -1;
        }
        return componentId[cell.x() + cell.z() * width];
    }

    public void setComponentId(GridCell cell, int id) {
        if (isInBounds(cell)) {
            componentId[cell.x() + cell.z() * width] = id;
        }
    }

    // --- Traversal queries ---

    /**
     * Convenience: is this cell walkable by a generic ground unit (CLEAR/RAMP, no clearance).
     */
    public boolean isWalkable(GridCell cell) {
        return isTraversable(cell, TraversalProfile.GROUND);
    }

    /**
     * Backward-compatible blocker toggle: block = OBSTACLE, unblock = CLEAR.
     */
    public void setBlocked(GridCell cell, boolean blocked) {
        setType(cell, blocked ? CellType.OBSTACLE : CellType.CLEAR);
    }

    /**
     * @return true if a unit with the given profile may occupy the cell (ignoring clearance)
     */
    public boolean isTraversable(GridCell cell, TraversalProfile profile) {
        if (!isInBounds(cell)) {
            return false;
        }
        return SurfaceMask.allows(profile.surfaces(), getType(cell));
    }

    /**
     * @return true if the unit's footprint (clearanceCells around the cell) fits entirely
     *         on traversable cells
     */
    public boolean hasClearance(GridCell cell, TraversalProfile profile) {
        int c = profile.clearanceCells();
        for (int dx = -c; dx <= c; dx++) {
            for (int dz = -c; dz <= c; dz++) {
                if (!isTraversable(new GridCell(cell.x() + dx, cell.z() + dz), profile)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Bresenham traversal between two cells (inclusive). Returns true only if every cell
     * along the line is walkable by a ground unit.
     */
    public boolean isLineWalkable(GridCell a, GridCell b) {
        return isLineTraversable(a, b, TraversalProfile.GROUND);
    }

    /**
     * Bresenham traversal (inclusive) checking traversal for the given profile, but ignoring
     * clearance (used for path smoothing, where clearance was already satisfied per-cell).
     */
    public boolean isLineTraversable(GridCell a, GridCell b, TraversalProfile profile) {
        int x0 = a.x();
        int z0 = a.z();
        int x1 = b.x();
        int z1 = b.z();
        int dx = Math.abs(x1 - x0);
        int dz = Math.abs(z1 - z0);
        int sx = x0 < x1 ? 1 : -1;
        int sz = z0 < z1 ? 1 : -1;
        int err = dx - dz;
        int x = x0;
        int z = z0;
        while (true) {
            if (!isTraversable(new GridCell(x, z), profile)) {
                return false;
            }
            if (x == x1 && z == z1) {
                break;
            }
            int e2 = 2 * err;
            if (e2 > -dz) {
                err -= dz;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                z += sz;
            }
        }
        return true;
    }
}
