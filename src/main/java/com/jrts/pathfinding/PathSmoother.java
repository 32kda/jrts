package com.jrts.pathfinding;

import java.util.ArrayList;
import java.util.List;

/**
 * String-pulling (line-of-sight) smoothing of a cell path.
 *
 * Walks forward from the current anchor and keeps the furthest cell that still has
 * walkable line-of-sight, producing a shorter path of straight segments while
 * preserving obstacle avoidance.
 */
public class PathSmoother {

    /**
     * @param path cell path from start to goal (both inclusive)
     * @return a reduced path where consecutive cells have walkable line-of-sight;
     *         first and last cells are preserved
     */
    public List<GridCell> smooth(Grid grid, List<GridCell> path, TraversalProfile profile) {
        if (path == null || path.isEmpty()) {
            return List.of();
        }
        if (path.size() <= 2) {
            return new ArrayList<>(path);
        }

        List<GridCell> result = new ArrayList<>();
        result.add(path.get(0));
        int anchor = 0;

        while (anchor < path.size() - 1) {
            int next = anchor + 1;
            for (int i = path.size() - 1; i > anchor; i--) {
                if (grid.isLineTraversable(path.get(anchor), path.get(i), profile)) {
                    next = i;
                    break;
                }
            }
            result.add(path.get(next));
            anchor = next;
        }

        return result;
    }
}
