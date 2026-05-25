package com.jrts.movement;

import com.jme3.math.Vector3f;

import java.util.List;

/**
 * Computes a path from start to end position.
 * Implementations:
 *   - SimpleLineNavigation  (Stage 1 mock)
 *   - AStarNavigation       (Stage 2+ real pathfinding)
 */
public interface NavigationService {

    /**
     * @param start current unit position
     * @param end   desired destination
     * @return ordered list of waypoints (start→...→end).
     *         Always non-null, always at least [end].
     */
    List<Vector3f> computePath(Vector3f start, Vector3f end);
}
