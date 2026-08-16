package com.jrts.map;

import java.nio.file.Path;

/**
 * Loads and saves maps. Loading reads JSON via {@link MapSerializer} and builds a runtime
 * {@link MapDefinition} via {@link MapFactory}.
 */
public class MapLoader {

    private final MapSerializer serializer = new MapSerializer();
    private final MapFactory factory = new MapFactory();

    /**
     * @param file path to a map JSON file
     * @return the runtime-ready map definition
     * @throws MapException on read/validation failure
     */
    public MapDefinition load(Path file) {
        MapData data = serializer.read(file);
        return factory.build(data, file.toAbsolutePath().getParent());
    }

    /**
     * Serialize a {@link MapData} to the given file.
     */
    public void save(Path file, MapData data) {
        serializer.write(file, data);
    }
}
