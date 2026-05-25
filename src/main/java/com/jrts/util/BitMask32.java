package com.jrts.util;

/**
 * 32-bit bitmask wrapper for efficient flag storage and manipulation.
 * All operations are O(1) and side-effect-free (returns new instance for mutations).
 */
public final class BitMask32 {

    private final int bits;

    public BitMask32() {
        this.bits = 0;
    }

    public BitMask32(int initialBits) {
        this.bits = initialBits;
    }

    public int getBits() {
        return bits;
    }

    public BitMask32 set(int flag) {
        return new BitMask32(bits | flag);
    }

    public BitMask32 clear(int flag) {
        return new BitMask32(bits & ~flag);
    }

    public BitMask32 toggle(int flag) {
        return new BitMask32(bits ^ flag);
    }

    public boolean has(int flag) {
        return (bits & flag) != 0;
    }

    public boolean hasAll(int mask) {
        return (bits & mask) == mask;
    }

    public boolean hasAny(int mask) {
        return (bits & mask) != 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BitMask32 other) {
            return bits == other.bits;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(bits);
    }

    @Override
    public String toString() {
        return String.format("BitMask32[0x%08X]", bits);
    }
}
