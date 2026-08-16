package com.jrts.building;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BuildingSmokeControlTest {

    @Test
    void smokeOnlyWithoutSmokePoint() {
        assertFalse(BuildingSmokeControl.shouldEmit(false, true, 0f, 0.5f));
    }

    @Test
    void producingEmitsSmoke() {
        assertTrue(BuildingSmokeControl.shouldEmit(true, true, 1f, 0.5f));
    }

    @Test
    void damagedEmitsSmoke() {
        assertTrue(BuildingSmokeControl.shouldEmit(true, false, 0.3f, 0.5f));
    }

    @Test
    void healthyIdleDoesNotEmit() {
        assertFalse(BuildingSmokeControl.shouldEmit(true, false, 0.9f, 0.5f));
    }
}
