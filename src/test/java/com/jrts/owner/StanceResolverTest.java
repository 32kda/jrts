package com.jrts.owner;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StanceResolverTest {

    @Test
    void neutralOwnerIsNeutralToEveryone() {
        assertEquals(Stance.NEUTRAL, StanceResolver.stanceOf(Owner.PLAYER, Owner.NEUTRAL));
        assertEquals(Stance.NEUTRAL, StanceResolver.stanceOf(2, Owner.NEUTRAL));
    }

    @Test
    void sameOwnerIsFriendly() {
        assertEquals(Stance.FRIEND, StanceResolver.stanceOf(Owner.PLAYER, Owner.PLAYER));
        assertEquals(Stance.FRIEND, StanceResolver.stanceOf(3, 3));
    }

    @Test
    void differentNonZeroOwnersAreEnemies() {
        assertEquals(Stance.ENEMY, StanceResolver.stanceOf(Owner.PLAYER, 2));
        assertEquals(Stance.ENEMY, StanceResolver.stanceOf(2, 3));
    }

    @Test
    void helperPredicates() {
        assertTrue(StanceResolver.isFriendly(1, 1));
        assertFalse(StanceResolver.isFriendly(1, 0));
        assertTrue(StanceResolver.isHostile(1, 2));
        assertFalse(StanceResolver.isHostile(1, 0));
    }

    @Test
    void ownerRangeValidation() {
        assertTrue(Owner.isValid(0));
        assertTrue(Owner.isValid(255));
        assertFalse(Owner.isValid(-1));
        assertFalse(Owner.isValid(256));
    }
}
