package com.jrts.map;

import com.jrts.pathfinding.CellType;
import com.jrts.pathfinding.Grid;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MapFactoryTest {

    private final MapFactory factory = new MapFactory();

    @Test
    void buildsFlatTerrainAndGridFromData() {
        MapData data = new MapData(1, "Flat", new MapTerrain(100f, 1f, -1000f, null, null, 1f),
                List.of(), List.of(), List.of(), List.of(), List.of());

        MapDefinition def = factory.build(data, null);

        assertEquals(0f, def.terrain().getHeight(10, 10), 0.001f);
        assertEquals(-50f, def.terrain().getMapMinX(), 0.001f);
        assertEquals(100, def.grid().getWidth());
        assertEquals(100, def.grid().getHeight());
    }

    @Test
    void bakesObjectsIntoGrid() {
        MapObject box = new MapObject("box", new float[]{10f, 0f}, new float[]{4f, 4f},
                null, 0f, 5f);
        MapData data = new MapData(1, "Objects", new MapTerrain(100f, 1f, -1000f, null, null, 1f),
                List.of(box), List.of(), List.of(), List.of(), List.of());

        MapDefinition def = factory.build(data, null);
        Grid grid = def.grid();

        assertEquals(CellType.OBSTACLE, grid.getType(grid.worldToCell(10f, 0f)));
        assertEquals(CellType.CLEAR, grid.getType(grid.worldToCell(40f, 40f)));
        assertEquals(1, def.obstacles().size());
    }

    @Test
    void buildingFootprintBlocksGridWithoutAppearingAsObstacle() {
        MapBuilding building = new MapBuilding("war_factory", 1,
                new float[]{30f, 0f, 30f}, 0f, null, new float[]{8f, 8f});
        MapData data = new MapData(1, "Buildings",
                new MapTerrain(100f, 1f, -1000f, null, null, 1f),
                List.of(), List.of(), List.of(), List.of(), List.of(building));

        MapDefinition def = factory.build(data, null);
        Grid grid = def.grid();

        assertFalse(grid.isWalkable(grid.worldToCell(30f, 30f)),
                "building footprint should block pathfinding");
        assertTrue(grid.isWalkable(grid.worldToCell(-30f, -30f)));
        assertTrue(def.obstacles().isEmpty(),
                "building footprint should not be listed as a rendered obstacle");
    }

    @Test
    void placementsArePassedThrough() {
        MapUnit unit = new MapUnit("heavy_tank", 1, new float[]{0f, 0f, 0f}, 0f, 400);
        MapBuilding building = new MapBuilding("war_factory", 1,
                new float[]{10f, 0f, 10f}, 0f, null, null);
        MapData data = new MapData(1, "Placements",
                new MapTerrain(100f, 1f, -1000f, null, null, 1f),
                List.of(), List.of(), List.of(), List.of(unit), List.of(building));

        MapDefinition def = factory.build(data, null);

        assertEquals(1, def.units().size());
        assertEquals(1, def.buildings().size());
        assertEquals(1, def.units().get(0).owner());
    }

    @Test
    void computesConnectedComponents() {
        // A closed box ring should split the map into at least two components.
        MapObject wall = new MapObject("box", new float[]{0f, 0f}, new float[]{10f, 10f},
                null, 0f, 5f);
        MapData data = new MapData(1, "Components",
                new MapTerrain(100f, 1f, -1000f, null, null, 1f),
                List.of(wall), List.of(), List.of(), List.of(), List.of());

        MapDefinition def = factory.build(data, null);
        Grid grid = def.grid();

        int inside = grid.getComponentId(grid.worldToCell(0f, 0f));
        int outside = grid.getComponentId(grid.worldToCell(40f, 40f));
        assertEquals(-1, inside, "center of the box is blocked, so no component");
        assertTrue(outside >= 0, "open ground should have a component id");
    }
}
