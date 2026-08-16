package com.jrts.pathfinding;

import java.util.ArrayList;
import java.util.List;

/**
 * A static obstacle whose 2D footprint is baked into a {@link Grid} as blocked cells.
 *
 * Two shapes, discriminated by {@link Shape}:
 * <ul>
 *   <li>BOX — axis-aligned box (center + half extents in XZ), optionally rotated by {@code yaw}</li>
 *   <li>CYLINDER — circle in XZ (center + radius)</li>
 * </ul>
 *
 * A rotated BOX is the universal primitive: a wall is a thin, long BOX with a yaw, and a
 * cube is an unrotated BOX. A cell is blocked when its center falls inside the obstacle
 * footprint expanded by {@code clearance} (the moving unit's radius), so A* corridors never
 * clip the obstacle.
 */
public class Obstacle {

    public enum Shape {
        BOX, CYLINDER
    }

    private final Shape shape;
    private final float cx;
    private final float cz;
    private final float halfX;
    private final float halfZ;
    private final float radius;
    private final float yaw;

    private Obstacle(Shape shape, float cx, float cz, float halfX, float halfZ, float radius,
                     float yaw) {
        this.shape = shape;
        this.cx = cx;
        this.cz = cz;
        this.halfX = halfX;
        this.halfZ = halfZ;
        this.radius = radius;
        this.yaw = yaw;
    }

    public static Obstacle box(float cx, float cz, float halfX, float halfZ) {
        return new Obstacle(Shape.BOX, cx, cz, halfX, halfZ, 0f, 0f);
    }

    public static Obstacle box(float cx, float cz, float halfX, float halfZ, float yaw) {
        return new Obstacle(Shape.BOX, cx, cz, halfX, halfZ, 0f, yaw);
    }

    public static Obstacle cylinder(float cx, float cz, float radius) {
        return new Obstacle(Shape.CYLINDER, cx, cz, 0f, 0f, radius, 0f);
    }

    public Shape getShape() {
        return shape;
    }

    public float getCenterX() {
        return cx;
    }

    public float getCenterZ() {
        return cz;
    }

    public float getHalfX() {
        return halfX;
    }

    public float getHalfZ() {
        return halfZ;
    }

    public float getRadius() {
        return radius;
    }

    public float getYaw() {
        return yaw;
    }

    /**
     * Mark every cell covered by this obstacle (plus clearance) as blocked.
     *
     * @return the cells that were marked, in row-major order (useful for assertions)
     */
    public List<GridCell> markBlocked(Grid grid, float clearance) {
        List<GridCell> marked = new ArrayList<>();
        float cellSize = grid.getCellSize();

        float minX = minWorldX() - clearance - cellSize;
        float maxX = maxWorldX() + clearance + cellSize;
        float minZ = minWorldZ() - clearance - cellSize;
        float maxZ = maxWorldZ() + clearance + cellSize;

        GridCell lo = grid.worldToCell(minX, minZ);
        GridCell hi = grid.worldToCell(maxX, maxZ);

        for (int x = lo.x(); x <= hi.x(); x++) {
            for (int z = lo.z(); z <= hi.z(); z++) {
                GridCell cell = new GridCell(x, z);
                if (!grid.isInBounds(cell)) {
                    continue;
                }
                if (covers(grid.cellCenterX(cell), grid.cellCenterZ(cell), clearance)) {
                    grid.setBlocked(cell, true);
                    marked.add(cell);
                }
            }
        }
        return marked;
    }

    private boolean covers(float wx, float wz, float clearance) {
        switch (shape) {
            case BOX: {
                float dx = wx - cx;
                float dz = wz - cz;
                float cos = (float) Math.cos(-yaw);
                float sin = (float) Math.sin(-yaw);
                float lx = dx * cos - dz * sin;
                float lz = dx * sin + dz * cos;
                return Math.abs(lx) <= halfX + clearance && Math.abs(lz) <= halfZ + clearance;
            }
            case CYLINDER: {
                float dx = wx - cx;
                float dz = wz - cz;
                float r = radius + clearance;
                return dx * dx + dz * dz <= r * r;
            }
            default:
                return false;
        }
    }

    private float halfWorldX() {
        if (shape != Shape.BOX) {
            return halfX;
        }
        float cos = Math.abs((float) Math.cos(yaw));
        float sin = Math.abs((float) Math.sin(yaw));
        return halfX * cos + halfZ * sin;
    }

    private float halfWorldZ() {
        if (shape != Shape.BOX) {
            return halfZ;
        }
        float cos = Math.abs((float) Math.cos(yaw));
        float sin = Math.abs((float) Math.sin(yaw));
        return halfX * sin + halfZ * cos;
    }

    private float minWorldX() {
        return shape == Shape.CYLINDER ? cx - radius : cx - halfWorldX();
    }

    private float maxWorldX() {
        return shape == Shape.CYLINDER ? cx + radius : cx + halfWorldX();
    }

    private float minWorldZ() {
        return shape == Shape.CYLINDER ? cz - radius : cz - halfWorldZ();
    }

    private float maxWorldZ() {
        return shape == Shape.CYLINDER ? cz + radius : cz + halfWorldZ();
    }
}
