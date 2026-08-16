package com.jrts.pathfinding;

import java.util.List;

/**
 * Result of a pathfinding query.
 *
 * @param found  true if a path was found
 * @param cells  ordered cells from start (inclusive) to goal (inclusive); empty on failure
 * @param reason failure reason constant when {@code found == false}, else null
 */
public record PathResult(boolean found, List<GridCell> cells, String reason) {

    public static final String START_BLOCKED = "START_BLOCKED";
    public static final String GOAL_BLOCKED = "GOAL_BLOCKED";
    public static final String UNREACHABLE = "UNREACHABLE";

    public static PathResult success(List<GridCell> cells) {
        return new PathResult(true, cells, null);
    }

    public static PathResult failure(String reason) {
        return new PathResult(false, List.of(), reason);
    }
}
