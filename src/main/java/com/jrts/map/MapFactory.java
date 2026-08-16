package com.jrts.map;

import com.jrts.pathfinding.ConnectedComponents;
import com.jrts.pathfinding.Grid;
import com.jrts.pathfinding.Obstacle;
import com.jrts.pathfinding.TraversalProfile;
import com.jrts.scene.HeightmapTerrainProvider;
import com.jrts.scene.TerrainGridBaker;
import com.jrts.scene.TerrainHeightProvider;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns a {@link MapData} into a runtime-ready {@link MapDefinition}: builds the terrain
 * provider, bakes the pathfinding grid (terrain + objects + building footprints), and
 * computes connected components.
 *
 * <p>Pure model transformation — no file I/O beyond resolving the heightmap path against the
 * supplied base directory.
 */
public class MapFactory {

    public MapDefinition build(MapData data, Path baseDir) {
        TerrainHeightProvider terrain = buildTerrain(data.terrain(), baseDir);
        float cellSize = data.terrain().cellSize();

        Grid grid = Grid.fromBounds(terrain.getMapMinX(), terrain.getMapMinZ(),
                terrain.getMapMaxX(), terrain.getMapMaxZ(), cellSize);

        TerrainGridBaker baker = new TerrainGridBaker();
        baker.classifyTerrain(grid, terrain);

        List<Obstacle> obstacles = new ArrayList<>();
        for (MapObject object : data.objects()) {
            Obstacle obstacle = toObstacle(object);
            obstacle.markBlocked(grid, 0f);
            obstacles.add(obstacle);
        }
        for (MapBuilding building : data.buildings()) {
            buildingFootprint(building).markBlocked(grid, 0f);
        }

        new ConnectedComponents().compute(grid, TraversalProfile.GROUND);

        return new MapDefinition(terrain, grid, obstacles, data.decorations(),
                data.logical(), data.units(), data.buildings());
    }

    private TerrainHeightProvider buildTerrain(MapTerrain terrain, Path baseDir) {
        if (terrain.hasHeightmap()) {
            Path png = baseDir == null
                    ? Path.of(terrain.heightmap())
                    : baseDir.resolve(terrain.heightmap());
            return HeightmapTerrainProvider.fromPng(terrain.size(), terrain.cellSize(),
                    terrain.waterLevel(), png, terrain.verticalScale());
        }
        if (terrain.hasInlineHeights()) {
            return HeightmapTerrainProvider.fromHeights(terrain.size(), terrain.cellSize(),
                    terrain.waterLevel(), terrain.heights());
        }
        return HeightmapTerrainProvider.flat(terrain.size(), 0f);
    }

    private Obstacle toObstacle(MapObject object) {
        if (object.isCylinder()) {
            return Obstacle.cylinder(object.centerX(), object.centerZ(),
                    object.radius() == null ? 0f : object.radius());
        }
        return Obstacle.box(object.centerX(), object.centerZ(),
                object.sizeX() / 2f, object.sizeZ() / 2f, object.yaw());
    }

    private Obstacle buildingFootprint(MapBuilding building) {
        return Obstacle.box(building.posX(), building.posZ(),
                building.sizeX() / 2f, building.sizeZ() / 2f, building.yaw());
    }
}
