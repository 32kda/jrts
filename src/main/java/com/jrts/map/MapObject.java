package com.jrts.map;

/**
 * A static, pathfinding-blocking shape placed on the map.
 *
 * <p>Box and cylinder are the two universal 2D primitives. A wall is a thin, rotated
 * box. These are primarily for special/testing setups; most map content is decorations,
 * buildings and units.
 *
 * <p>Field layout depends on {@code type}:
 * <ul>
 *   <li>{@code "box"} — {@code center} [x,z], {@code size} [width,depth], {@code yaw} radians</li>
 *   <li>{@code "cylinder"} — {@code center} [x,z], {@code radius}</li>
 * </ul>
 */
public record MapObject(
        String type,
        float[] center,
        float[] size,
        Float radius,
        Float yaw,
        Float height) {

    public static final String TYPE_BOX = "box";
    public static final String TYPE_CYLINDER = "cylinder";
    public static final float DEFAULT_HEIGHT = 5f;

    public MapObject {
        if (type == null || type.isBlank()) {
            type = TYPE_BOX;
        }
        if (yaw == null) {
            yaw = 0f;
        }
        if (height == null) {
            height = DEFAULT_HEIGHT;
        }
    }

    public boolean isBox() {
        return TYPE_BOX.equalsIgnoreCase(type);
    }

    public boolean isCylinder() {
        return TYPE_CYLINDER.equalsIgnoreCase(type);
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
