package com.jrts.map;

/**
 * A purely visual prop placed on the map. References a model by name; carries no
 * pathfinding footprint and no game logic.
 */
public record MapDecoration(
        String model,
        float[] position,
        Float yaw,
        Float scale) {

    public static final float DEFAULT_SCALE = 1f;

    public MapDecoration {
        if (yaw == null) {
            yaw = 0f;
        }
        if (scale == null) {
            scale = DEFAULT_SCALE;
        }
    }

    public float posX() {
        return position == null ? 0f : position[0];
    }

    public float posY() {
        return position == null || position.length < 2 ? 0f : position[1];
    }

    public float posZ() {
        return position == null || position.length < 3 ? 0f : position[2];
    }
}
