package com.jrts.scene;

import com.jrts.pathfinding.CellType;
import com.jrts.pathfinding.ConnectedComponents;
import com.jrts.pathfinding.Grid;
import com.jrts.pathfinding.GridCell;
import com.jrts.pathfinding.Obstacle;
import com.jrts.pathfinding.TraversalProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Bakes a {@link TerrainHeightProvider} plus static obstacles into a pathfinding {@link Grid}.
 *
 * This is the seam between terrain sources and search: the search algorithms only read cell
 * metadata, so a blocked cell is identical whether it came from water, a cliff, or an obstacle.
 *
 * Classification per cell:
 * <ul>
 *   <li>out of bounds → {@link CellType#IMPASSABLE}</li>
 *   <li>water → {@link CellType#WATER}</li>
 *   <li>gradient above {@link #MAX_WALKABLE_GRADIENT} → {@link CellType#CLIFF}</li>
 *   <li>otherwise → {@link CellType#CLEAR}, with the terrain height stored on the cell</li>
 * </ul>
 *
 * Ramps are CLEAR cells whose height changes gradually; per-edge slope is enforced later by
 * {@code TraversalProfile.maxClimb} during A*. Explicit ramp classification arrives with real
 * heightmaps (M4).
 */
public class TerrainGridBaker {

    private static final Logger log = LoggerFactory.getLogger(TerrainGridBaker.class);

    /** Gradient magnitude above which a cell is treated as an impassable cliff. */
    public static final float MAX_WALKABLE_GRADIENT = 0.7f;

    private final ConnectedComponents connectedComponents = new ConnectedComponents();

    /**
     * @param terrain   height/water/gradient source
     * @param cellSize  world units per pathfinding cell
     * @param obstacles static obstacles to bake as {@link CellType#OBSTACLE} (exact footprint)
     * @return a ground-layer grid with classified cells and computed components
     */
    public Grid bake(TerrainHeightProvider terrain, float cellSize, List<Obstacle> obstacles) {
        Grid grid = Grid.fromBounds(
                terrain.getMapMinX(), terrain.getMapMinZ(),
                terrain.getMapMaxX(), terrain.getMapMaxZ(), cellSize);
        classifyTerrain(grid, terrain);

        for (Obstacle obstacle : obstacles) {
            obstacle.markBlocked(grid, 0f);
        }

        int components = connectedComponents.compute(grid, TraversalProfile.GROUND);
        log.info("Baked pathfinding grid {}x{} cells, cellSize={}, {} components",
                grid.getWidth(), grid.getHeight(), cellSize, components);
        return grid;
    }

    /**
     * Classify terrain cells in place (height + water/cliff type), without touching obstacles
     * or connected components.
     */
    public void classifyTerrain(Grid grid, TerrainHeightProvider terrain) {
        for (int z = 0; z < grid.getHeight(); z++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                GridCell cell = new GridCell(x, z);
                float wx = grid.cellCenterX(cell);
                float wz = grid.cellCenterZ(cell);
                grid.setHeight(cell, terrain.getHeight(wx, wz));

                if (!terrain.isInBounds(wx, wz)) {
                    grid.setType(cell, CellType.IMPASSABLE);
                } else if (terrain.isWater(wx, wz)) {
                    grid.setType(cell, CellType.WATER);
                } else if (terrain.getGradient(wx, wz) > MAX_WALKABLE_GRADIENT) {
                    grid.setType(cell, CellType.CLIFF);
                } else {
                    grid.setType(cell, CellType.CLEAR);
                }
            }
        }
        log.debug("Classified terrain on {}x{} grid", grid.getWidth(), grid.getHeight());
    }
}
