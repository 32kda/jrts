package com.jrts.map;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MapLoaderTest {

    private final MapLoader loader = new MapLoader();

    @Test
    void loadsShippedTestMap() {
        MapDefinition map = loader.load(Path.of("assets/maps/test_map.json"));

        assertNotNull(map.terrain());
        assertEquals(1000f, map.terrain().getMapMaxX() - map.terrain().getMapMinX(), 0.001f);

        assertEquals(3, map.obstacles().size());
        assertEquals(1, map.units().size());
        assertEquals(1, map.buildings().size());
        assertEquals(1, map.logicalAreas().size());

        assertFalse(map.grid().isWalkable(map.grid().worldToCell(30f, 0f)),
                "box object cell should be blocked");
        assertFalse(map.grid().isWalkable(map.grid().worldToCell(-20f, 20f)),
                "cylinder object cell should be blocked");
        assertFalse(map.grid().isWalkable(map.grid().worldToCell(100f, 50f)),
                "building footprint should be blocked");
    }
}
