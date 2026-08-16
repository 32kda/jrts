package com.jrts.pathfinding;

import com.jrts.util.OctileDistance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/**
 * A* over a {@link Grid} with 8-directional movement and octile heuristic.
 *
 * Clean-room implementation of the standard algorithm, extended for unit traversal:
 * <ul>
 *   <li>surface mask — a cell is traversable only if the unit's surfaces allow its type</li>
 *   <li>clearance — a cell is expanded only if the unit's footprint fits</li>
 *   <li>slope — an edge is blocked when the height change exceeds {@code maxClimb}</li>
 *   <li>cost — per-cell movement cost multiplier (&ge; 1)</li>
 * </ul>
 *
 * Deterministic: ties broken by f, then by node index.
 */
public class AStarPathfinder implements Pathfinder {

    private static final float ORTHO = 10f;
    private static final float DIAG = 14f;

    private static final int[] DX = {1, -1, 0, 0, 1, 1, -1, -1};
    private static final int[] DZ = {0, 0, 1, -1, 1, -1, 1, -1};

    @Override
    public PathResult findPath(Grid grid, GridCell start, GridCell goal, TraversalProfile profile) {
        return findPath(grid, start, goal, profile,
                0, 0, grid.getWidth() - 1, grid.getHeight() - 1);
    }

    /**
     * A* restricted to the cell rectangle [minX, maxX] x [minZ, maxZ] (inclusive). Used by
     * hierarchical search to compute intra-cluster paths.
     *
     * @return a path from start (inclusive) to goal (inclusive), or a failure result
     */
    public PathResult findPath(Grid grid, GridCell start, GridCell goal, TraversalProfile profile,
                               int minX, int minZ, int maxX, int maxZ) {
        if (!grid.isTraversable(start, profile)) {
            return PathResult.failure(PathResult.START_BLOCKED);
        }
        if (!grid.hasClearance(goal, profile)) {
            return PathResult.failure(PathResult.GOAL_BLOCKED);
        }
        if (start.equals(goal)) {
            return PathResult.success(List.of(start));
        }

        int width = grid.getWidth();
        int size = width * grid.getHeight();

        float[] gScore = new float[size];
        Arrays.fill(gScore, Float.POSITIVE_INFINITY);
        boolean[] closed = new boolean[size];
        int[] cameFrom = new int[size];
        Arrays.fill(cameFrom, -1);

        int startIdx = index(start, width);
        int goalIdx = index(goal, width);
        gScore[startIdx] = 0f;

        PriorityQueue<Long> open = new PriorityQueue<>();
        open.add(pack(heuristic(startIdx, goalIdx, width), startIdx));

        while (!open.isEmpty()) {
            int current = unpackIndex(open.poll());
            if (closed[current]) {
                continue;
            }
            if (current == goalIdx) {
                return PathResult.success(reconstruct(cameFrom, startIdx, goalIdx, width));
            }
            closed[current] = true;

            int cx = current % width;
            int cz = current / width;
            float curHeight = grid.getHeight(new GridCell(cx, cz));

            for (int d = 0; d < 8; d++) {
                int nx = cx + DX[d];
                int nz = cz + DZ[d];
                if (nx < minX || nx > maxX || nz < minZ || nz > maxZ) {
                    continue;
                }
                GridCell neighbor = new GridCell(nx, nz);
                if (!grid.hasClearance(neighbor, profile)) {
                    continue;
                }
                if (Math.abs(grid.getHeight(neighbor) - curHeight) > profile.maxClimb()) {
                    continue;
                }
                if (d >= 4) {
                    // Diagonal: prevent corner cutting through a blocked orthogonal cell.
                    if (!grid.isTraversable(new GridCell(cx + DX[d], cz), profile)
                            || !grid.isTraversable(new GridCell(cx, cz + DZ[d]), profile)) {
                        continue;
                    }
                }
                int neighborIdx = index(neighbor, width);
                if (closed[neighborIdx]) {
                    continue;
                }
                float stepCost = (d >= 4 ? DIAG : ORTHO) * grid.getCost(neighbor);
                float tentative = gScore[current] + stepCost;
                if (tentative < gScore[neighborIdx]) {
                    gScore[neighborIdx] = tentative;
                    cameFrom[neighborIdx] = current;
                    open.add(pack(tentative + heuristic(neighborIdx, goalIdx, width), neighborIdx));
                }
            }
        }

        return PathResult.failure(PathResult.UNREACHABLE);
    }

    private float heuristic(int nodeIdx, int goalIdx, int width) {
        return OctileDistance.distance(
                nodeIdx % width, nodeIdx / width,
                goalIdx % width, goalIdx / width) * ORTHO;
    }

    private static int index(GridCell cell, int width) {
        return cell.x() + cell.z() * width;
    }

    private static GridCell cell(int idx, int width) {
        return new GridCell(idx % width, idx / width);
    }

    /**
     * Pack {@code (f, index)} into a single long: f's float bits in the high half,
     * index in the low half. f is always non-negative, so bit ordering is monotonic
     * and the heap pops lowest-f / lowest-index first.
     */
    private static long pack(float f, int index) {
        return ((long) Float.floatToIntBits(f) << 32) | (index & 0xFFFFFFFFL);
    }

    private static int unpackIndex(long packed) {
        return (int) (packed & 0xFFFFFFFFL);
    }

    private static List<GridCell> reconstruct(int[] cameFrom, int startIdx, int goalIdx, int width) {
        List<GridCell> cells = new ArrayList<>();
        int cur = goalIdx;
        while (cur != -1) {
            cells.add(cell(cur, width));
            cur = cameFrom[cur];
        }
        Collections.reverse(cells);
        return cells;
    }
}
