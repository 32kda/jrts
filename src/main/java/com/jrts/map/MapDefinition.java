package com.jrts.map;

import com.jrts.pathfinding.Grid;
import com.jrts.pathfinding.Obstacle;
import com.jrts.scene.TerrainHeightProvider;

import java.util.List;

/**
 * A map fully resolved for runtime use: terrain provider, baked pathfinding grid, blocking
 * obstacles, decorations, logical areas, and unit/building placements.
 *
 * <p>Produced by {@link MapFactory} from a {@link MapData}. Building footprints are baked into
 * the grid but are not part of {@link #obstacles()} (buildings render themselves).
 */
public record MapDefinition(
        TerrainHeightProvider terrain,
        Grid grid,
        List<Obstacle> obstacles,
        List<MapDecoration> decorations,
        List<LogicalArea> logicalAreas,
        List<MapUnit> units,
        List<MapBuilding> buildings) {
}
