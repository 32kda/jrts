package com.jrts.unit;

/**
 * 32-bit flag constants for runtime units.
 * Each flag is a power-of-two bit.
 */
public final class UnitFlags {

    private UnitFlags() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static final int CAN_MOVE = 0x0001;
    public static final int CAN_BE_SELECTED = 0x0002;
    public static final int CAN_BE_PICKED = 0x0004;
    public static final int IS_STRUCTURE = 0x0008;
    public static final int IS_PROJECTILE = 0x0010;
    public static final int SELECTABLE = 0x0020;
    public static final int AIRBORNE = 0x0100;
    public static final int DOCKABLE = 0x0200;
    public static final int CAN_BUILD = 0x0400;
    public static final int CAN_GATHER = 0x0800;
    public static final int HAS_TURRET = 0x1000;
    public static final int STICK_TO_GROUND = 0x2000;

    /**
     * Build a flags bitmask from a UnitConfig.
     *
     * @param config the parsed unit configuration
     * @return combined bitmask of applicable flags
     */
    public static int fromUnitConfig(com.jrts.config.UnitConfig config) {
        int f = 0;
        if (!config.isStructure()) {
            f |= CAN_MOVE;
        }
        if (config.identity().selectable()) {
            f |= CAN_BE_SELECTED | CAN_BE_PICKED | SELECTABLE;
        }
        if (config.isStructure()) {
            f |= IS_STRUCTURE;
        }
        if (config.turrets() != null && config.turrets().turret()) {
            f |= HAS_TURRET;
        }
        if (!config.isStructure()) {
            f |= STICK_TO_GROUND;
        }
        return f;
    }
}
