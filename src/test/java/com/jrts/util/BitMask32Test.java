package com.jrts.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BitMask32Test {

    @Test
    void defaultConstructorHasZeroBits() {
        BitMask32 mask = new BitMask32();
        assertEquals(0, mask.getBits());
    }

    @Test
    void constructorWithBitsPreservesValue() {
        BitMask32 mask = new BitMask32(0xABCD);
        assertEquals(0xABCD, mask.getBits());
    }

    @Test
    void setAddsFlag() {
        BitMask32 mask = new BitMask32(0);
        mask = mask.set(0x0001);
        assertTrue(mask.has(0x0001));
    }

    @Test
    void clearRemovesFlag() {
        BitMask32 mask = new BitMask32(0x000F);
        mask = mask.clear(0x0001);
        assertFalse(mask.has(0x0001));
        assertTrue(mask.has(0x0002));
    }

    @Test
    void toggleFlipsFlag() {
        BitMask32 mask = new BitMask32(0x0001);
        mask = mask.toggle(0x0001);
        assertFalse(mask.has(0x0001));
        mask = mask.toggle(0x0001);
        assertTrue(mask.has(0x0001));
    }

    @Test
    void hasAllWhenAllBitsPresent() {
        BitMask32 mask = new BitMask32(0x0007);
        assertTrue(mask.hasAll(0x0003));
        assertFalse(mask.hasAll(0x0008));
    }

    @Test
    void hasAnyWhenAnyBitPresent() {
        BitMask32 mask = new BitMask32(0x0003);
        assertTrue(mask.hasAny(0x0005));
        assertFalse(mask.hasAny(0x0008));
    }

    @Test
    void immutabilityReturnsNewInstance() {
        BitMask32 original = new BitMask32(0);
        BitMask32 modified = original.set(0x0001);
        assertNotSame(original, modified);
        assertEquals(0, original.getBits());
        assertEquals(0x0001, modified.getBits());
    }

    @Test
    void equalsAndHashCode() {
        BitMask32 a = new BitMask32(0xFFFF);
        BitMask32 b = new BitMask32(0xFFFF);
        BitMask32 c = new BitMask32(0xFFF0);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }
}
