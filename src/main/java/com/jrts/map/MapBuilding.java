package com.jrts.map;

/**
 * A building placement in a map file. Mirrors {@link MapUnit}: config {@code type},
 * {@code owner} (byte player id), {@code position}, {@code yaw}, optional {@code health}
 * override, plus an optional {@code size} [width,depth] footprint override used to block
 * the pathfinding grid.
 *
 * <p>{@code type} resolves to {@code assets/config/buildings/&lt;type&gt;.toml}.
 */
public record MapBuilding(
        String type,
        Integer owner,
        float[] position,
        Float yaw,
        Integer health,
        float[] size) {

    public static final float DEFAULT_FOOTPRINT = 12f;

    public MapBuilding {
        if (owner == null) {
            owner = 0;
        }
        if (yaw == null) {
            yaw = 0f;
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

    public float sizeX() {
        return size == null ? DEFAULT_FOOTPRINT : size[0];
    }

    public float sizeZ() {
        return size == null ? DEFAULT_FOOTPRINT : (size.length > 1 ? size[1] : size[0]);
    }
}
