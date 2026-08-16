package com.jrts.scene;

import com.jrts.pathfinding.ConnectedComponents;
import com.jrts.pathfinding.Grid;
import com.jrts.pathfinding.Obstacle;
import com.jrts.pathfinding.TraversalProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * The test/demo map: terrain, pathfinding grid, and a few static obstacles.
 *
 * Encapsulates map creation so the composition root ({@code Main}) stays a thin wiring layer
 * instead of constructing terrain and obstacles inline. When real maps arrive (heightmap,
 * water, cliffs), this is replaced by a {@code MapDefinition} / {@code HeightmapMap} without
 * touching the systems that consume {@link #terrain()} and {@link #grid()}.
 */
public class TestMap {

    private static final Logger log = LoggerFactory.getLogger(TestMap.class);

    private static final float MAP_SIZE = 500f;
    private static final float CELL_SIZE = 1f;

    private final TerrainHeightProvider terrain;
    private final Grid grid;
    private final List<Obstacle> obstacles;

    public TestMap() {
        this.terrain = new FlatTerrainHeightProvider(0f, MAP_SIZE);
        this.grid = Grid.fromBounds(
                terrain.getMapMinX(), terrain.getMapMinZ(),
                terrain.getMapMaxX(), terrain.getMapMaxZ(), CELL_SIZE);
        new TerrainGridBaker().classifyTerrain(grid, terrain);

        this.obstacles = List.of(
                Obstacle.box(30f, 0f, 5f, 5f),
                Obstacle.cylinder(-20f, 20f, 5f),
                Obstacle.box(30f, -20f, 20f, 1f));

        for (Obstacle obstacle : obstacles) {
            obstacle.markBlocked(grid, 0f);
        }
        new ConnectedComponents().compute(grid, TraversalProfile.GROUND);

        log.info("TestMap created: {} obstacles on {}x{} grid",
                obstacles.size(), grid.getWidth(), grid.getHeight());
    }

    public TerrainHeightProvider terrain() {
        return terrain;
    }

    public Grid grid() {
        return grid;
    }

    public List<Obstacle> obstacles() {
        return obstacles;
    }
}
