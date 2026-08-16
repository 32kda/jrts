package com.jrts.map;

/**
 * A unit placement in a map file. Carries the same properties a unit has in-game:
 * its config {@code type}, {@code owner} (byte player id), world {@code position},
 * {@code yaw}, and an optional {@code health} override.
 *
 * <p>{@code type} resolves to {@code assets/config/units/&lt;type&gt;.toml}; {@code owner}
 * defaults to {@code 0} (neutral) when omitted; {@code health} defaults to the config's
 * strength when omitted.
 */
public record MapUnit(
        String type,
        Integer owner,
        float[] position,
        Float yaw,
        Integer health) {

    public MapUnit {
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
}
