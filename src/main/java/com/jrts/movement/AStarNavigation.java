package com.jrts.movement;

import com.jrts.camera.ScreenMap;
import com.jrts.pathfinding.Grid;
import com.jrts.pathfinding.GridCell;
import com.jrts.pathfinding.PathResult;
import com.jrts.pathfinding.PathSmoother;
import com.jrts.pathfinding.Pathfinder;
import com.jrts.pathfinding.TraversalProfile;
import com.jrts.scene.TerrainHeightProvider;
import com.jme3.math.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Grid-based {@link NavigationService} implementation.
 *
 * Bridges the engine-independent pathfinding module (grid + pathfinder + smoothing) to the
 * JME world: converts world positions to grid cells, runs the configured {@link Pathfinder}
 * (fine A* or hierarchical) for the given {@link TraversalProfile}, smooths the path, and
 * returns ordered world-space waypoints.
 *
 * If the goal cell is blocked/unreachable, falls back to the nearest reachable cell so
 * a unit ordered into an obstacle still moves close instead of freezing.
 */
public class AStarNavigation implements NavigationService {

    private static final Logger log = LoggerFactory.getLogger(AStarNavigation.class);

    private final Grid grid;
    private final Pathfinder pathfinder;
    private final PathSmoother smoother;
    private final TerrainHeightProvider terrain;
    private final TraversalProfile profile;

    public AStarNavigation(Grid grid, Pathfinder pathfinder, PathSmoother smoother,
                           TerrainHeightProvider terrain, TraversalProfile profile) {
        this.grid = grid;
        this.pathfinder = pathfinder;
        this.smoother = smoother;
        this.terrain = terrain;
        this.profile = profile;
        log.info("AStarNavigation initialized on {}x{} grid, surfaces={}, clearance={}",
                grid.getWidth(), grid.getHeight(), profile.surfaces(), profile.clearanceCells());
    }

    @Override
    public List<Vector3f> computePath(Vector3f start, Vector3f end) {
        Vector3f clampedEnd = ScreenMap.clampToMap(end, terrain);

        GridCell startCell = grid.worldToCell(start.x, start.z);
        GridCell goalCell = grid.worldToCell(clampedEnd.x, clampedEnd.z);

        if (startCell.equals(goalCell)) {
            return List.of(clampedEnd);
        }

        PathResult result = pathfinder.findPath(grid, startCell, goalCell, profile);
        if (!result.found()) {
            GridCell nearest = findNearestReachableCell(startCell, goalCell);
            if (nearest == null) {
                log.warn("No reachable cell near goal {}, returning straight line", goalCell);
                return List.of(clampedEnd);
            }
            result = pathfinder.findPath(grid, startCell, nearest, profile);
            if (!result.found()) {
                return List.of(clampedEnd);
            }
        }

        List<GridCell> smoothed = smoother.smooth(grid, result.cells(), profile);
        List<Vector3f> waypoints = new ArrayList<>();
        for (int i = 1; i < smoothed.size(); i++) {
            GridCell c = smoothed.get(i);
            float wx = grid.cellCenterX(c);
            float wz = grid.cellCenterZ(c);
            waypoints.add(new Vector3f(wx, terrain.getHeight(wx, wz), wz));
        }
        log.debug("Computed path with {} waypoints", waypoints.size());
        return waypoints;
    }

    /**
     * BFS outward from the goal cell to find the closest cell that is both traversable and
     * reachable from the start. Returns null if none exists within the grid.
     */
    private GridCell findNearestReachableCell(GridCell startCell, GridCell goalCell) {
        int maxRadius = Math.max(grid.getWidth(), grid.getHeight());
        for (int r = 0; r <= maxRadius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    GridCell candidate = new GridCell(goalCell.x() + dx, goalCell.z() + dz);
                    if (grid.isTraversable(candidate, profile)
                            && pathfinder.findPath(grid, startCell, candidate, profile).found()) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }
}
