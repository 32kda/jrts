package com.jrts.map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MapSerializerTest {

    private final MapSerializer serializer = new MapSerializer();

    @Test
    void roundTripsFullMap(@TempDir Path tempDir) throws Exception {
        MapData original = sampleMap();

        Path file = tempDir.resolve("map.json");
        serializer.write(file, original);

        MapData loaded = serializer.read(file);
        assertEquals(serializer.toJson(original), serializer.toJson(loaded));
        assertEquals("Test Map", loaded.name());
        assertEquals(1, loaded.units().size());
        assertEquals(1, loaded.units().get(0).owner());
        assertEquals(1, loaded.buildings().size());
        assertEquals(1, loaded.logical().size());
        assertArrayEquals(new float[]{10f, 0f}, loaded.objects().get(0).center());
    }

    @Test
    void serializationIsStableAcrossRoundTrip() {
        MapData original = sampleMap();
        String first = serializer.toJson(original);
        String second = serializer.toJson(serializerFromJson(first));
        assertEquals(first, second);
    }

    @Test
    void defaultsAreAppliedWhenFieldsAreMissing() {
        MapData loaded = serializerFromJson("{\"name\":\"Minimal\"}");
        assertEquals(MapData.CURRENT_VERSION, loaded.version());
        assertEquals("Minimal", loaded.name());
        assertNotNull(loaded.terrain());
        assertEquals(1000f, loaded.terrain().size(), 0.001f);
        assertTrue(loaded.units().isEmpty());
        assertTrue(loaded.buildings().isEmpty());
    }

    @Test
    void rejectsUnsupportedVersion(@TempDir Path tempDir) throws Exception {
        MapData future = new MapData(MapData.CURRENT_VERSION + 1, "Future", null,
                List.of(), List.of(), List.of(), List.of(), List.of());
        Path file = tempDir.resolve("future.json");
        serializer.write(file, future);
        assertThrows(MapException.class, () -> serializer.read(file));
    }

    @Test
    void missingFileThrows(@TempDir Path tempDir) {
        assertThrows(MapException.class,
                () -> serializer.read(tempDir.resolve("does_not_exist.json")));
    }

    @Test
    void malformedJsonThrows(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("broken.json");
        Files.writeString(file, "{ not valid json");
        assertThrows(MapException.class, () -> serializer.read(file));
    }

    private MapData serializerFromJson(String json) {
        try {
            Path temp = Files.createTempFile("map", ".json");
            Files.writeString(temp, json);
            return serializer.read(temp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private MapData sampleMap() {
        MapTerrain terrain = new MapTerrain(200f, 2f, -100f, new float[]{0f, 0f, 0f, 0f},
                null, 1f);
        MapObject box = new MapObject("box", new float[]{10f, 0f}, new float[]{4f, 4f},
                null, 0.5f, 5f);
        MapObject cyl = new MapObject("cylinder", new float[]{-5f, 5f}, null, 3f, null, null);
        MapDecoration deco = new MapDecoration("rock_01", new float[]{1f, 2f, 3f}, 0f, 2f);
        LogicalArea area = new LogicalArea("zone", "box", new float[]{0f, 0f},
                new float[]{10f, 10f}, null, 0f);
        MapUnit unit = new MapUnit("heavy_tank", 1, new float[]{0f, 0f, 0f}, 0.5f, 400);
        MapBuilding building = new MapBuilding("war_factory", 1,
                new float[]{20f, 0f, 20f}, 0f, 1200, new float[]{12f, 12f});

        return new MapData(MapData.CURRENT_VERSION, "Test Map", terrain,
                List.of(box, cyl), List.of(deco), List.of(area), List.of(unit),
                List.of(building));
    }
}
