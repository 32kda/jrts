package com.jrts.map;

import java.util.List;

/**
 * Root object of a map file. Serializable to/from JSON.
 *
 * <p>Structure:
 * <ul>
 *   <li>{@code terrain} — height source and water level</li>
 *   <li>{@code objects} — blocking 2D shapes (box/cylinder), special/testing use</li>
 *   <li>{@code decorations} — render-only props</li>
 *   <li>{@code logical} — named 2D regions for mission scripting</li>
 *   <li>{@code units} — unit placements with in-game properties</li>
 *   <li>{@code buildings} — building placements</li>
 * </ul>
 */
public record MapData(
        Integer version,
        String name,
        MapTerrain terrain,
        List<MapObject> objects,
        List<MapDecoration> decorations,
        List<LogicalArea> logical,
        List<MapUnit> units,
        List<MapBuilding> buildings) {

    public static final int CURRENT_VERSION = 1;

    public MapData {
        if (version == null) {
            version = CURRENT_VERSION;
        }
        if (name == null || name.isBlank()) {
            name = "Untitled";
        }
        if (terrain == null) {
            terrain = new MapTerrain(null, null, null, null, null, null);
        }
        objects = objects == null ? List.of() : objects;
        decorations = decorations == null ? List.of() : decorations;
        logical = logical == null ? List.of() : logical;
        units = units == null ? List.of() : units;
        buildings = buildings == null ? List.of() : buildings;
    }
}
