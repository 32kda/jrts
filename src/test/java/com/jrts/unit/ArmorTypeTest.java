package com.jrts.unit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArmorTypeTest {

    @Test
    void fromStringExactMatch() {
        assertEquals(ArmorType.HEAVY, ArmorType.fromString("heavy"));
    }

    @Test
    void fromStringCaseInsensitive() {
        assertEquals(ArmorType.LIGHT, ArmorType.fromString("LIGHT"));
        assertEquals(ArmorType.WOOD, ArmorType.fromString("Wood"));
    }

    @Test
    void fromStringTrimsWhitespace() {
        assertEquals(ArmorType.CONCRETE, ArmorType.fromString("  concrete  "));
    }

    @Test
    void fromStringNullReturnsNone() {
        assertEquals(ArmorType.NONE, ArmorType.fromString(null));
    }

    @Test
    void fromStringEmptyReturnsNone() {
        assertEquals(ArmorType.NONE, ArmorType.fromString(""));
    }

    @Test
    void fromStringUnknownThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> ArmorType.fromString("adamantium"));
    }

    @Test
    void allValuesHaveNonNullKey() {
        for (ArmorType type : ArmorType.values()) {
            assertNotNull(type.getKey());
            assertFalse(type.getKey().isEmpty());
        }
    }
}
