package com.jrts.map;

/**
 * A named 2D region used by mission scripting (trigger zones, harvest areas, etc.).
 *
 * <p>Logical areas are stored but neither rendered nor baked into the pathfinding grid.
 * Shape follows the same convention as {@link MapObject}: {@code box} uses {@code center}
 * and {@code size}, {@code cylinder} uses {@code center} and {@code radius}.
 */
public record LogicalArea(
        String name,
        String type,
        float[] center,
        float[] size,
        Float radius,
        Float yaw) {

    public LogicalArea {
        if (type == null || type.isBlank()) {
            type = MapObject.TYPE_BOX;
        }
        if (yaw == null) {
            yaw = 0f;
        }
    }

    public boolean isBox() {
        return MapObject.TYPE_BOX.equalsIgnoreCase(type);
    }

    public float centerX() {
        return center == null ? 0f : center[0];
    }

    public float centerZ() {
        return center == null ? 0f : (center.length > 1 ? center[1] : 0f);
    }

    public float sizeX() {
        return size == null ? 0f : size[0];
    }

    public float sizeZ() {
        return size == null ? 0f : (size.length > 1 ? size[1] : 0f);
    }
}
