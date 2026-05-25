package com.jrts.unit;

/**
 * Armor types matching the spec's armor classification.
 * Determines damage resistance via ArmorTable (future Stage 2+).
 */
public enum ArmorType {
    NONE("none"),
    WOOD("wood"),
    LIGHT("light"),
    HEAVY("heavy"),
    CONCRETE("concrete");

    private final String key;

    ArmorType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    /**
     * Parse a TOML armor string to enum, case-insensitive.
     *
     * @param value TOML armor value (e.g. "heavy")
     * @return matching ArmorType
     * @throws IllegalArgumentException if value is unrecognized
     */
    public static ArmorType fromString(String value) {
        if (value == null || value.isEmpty()) {
            return NONE;
        }
        for (ArmorType type : values()) {
            if (type.key.equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown armor type: " + value);
    }
}
