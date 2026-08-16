package com.jrts.map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads and writes {@link MapData} as JSON using Gson.
 *
 * <p>Pure persistence concern: no game logic, no runtime object construction. Rejects
 * files whose {@code version} differs from {@link MapData#CURRENT_VERSION} so the loader
 * never silently misinterprets a future schema.
 */
public class MapSerializer {

    private static final Logger log = LoggerFactory.getLogger(MapSerializer.class);

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Read a map JSON file into a {@link MapData}.
     *
     * @throws MapException if the file is missing, malformed, or of an unsupported version
     */
    public MapData read(Path file) {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            MapData data = gson.fromJson(reader, MapData.class);
            if (data == null) {
                throw new MapException("Map file is empty: " + file);
            }
            if (data.version() != MapData.CURRENT_VERSION) {
                throw new MapException("Unsupported map version " + data.version()
                        + " (expected " + MapData.CURRENT_VERSION + "): " + file);
            }
            log.info("Loaded map '{}' from {}", data.name(), file);
            return data;
        } catch (JsonParseException e) {
            throw new MapException("Malformed map file: " + file, e);
        } catch (IOException e) {
            throw new MapException("Failed to read map file: " + file, e);
        }
    }

    /**
     * Serialize a {@link MapData} to JSON and write it to the given file.
     */
    public void write(Path file, MapData data) {
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            gson.toJson(data, writer);
            writer.newLine();
            log.info("Saved map '{}' to {}", data.name(), file);
        } catch (IOException e) {
            throw new MapException("Failed to write map file: " + file, e);
        }
    }

    /**
     * @return the JSON representation of the given data (for tests / debugging)
     */
    public String toJson(MapData data) {
        return gson.toJson(data);
    }
}
