package com.jrts.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnimationControllerTest {

    @Test
    void idleWhenNoConditionsActive() {
        AnimationController controller = new AnimationController();
        assertEquals(ModelCondition.IDLE, controller.currentCondition());
        assertTrue(controller.currentClipName().isEmpty());
    }

    @Test
    void singleActiveConditionWins() {
        AnimationController controller = new AnimationController();
        controller.setActive(ModelCondition.MOVING, true);
        assertEquals(ModelCondition.MOVING, controller.currentCondition());
    }

    @Test
    void highestPriorityConditionWins() {
        AnimationController controller = new AnimationController();
        controller.setActive(ModelCondition.MOVING, true);
        controller.setActive(ModelCondition.FIRING, true);
        controller.setActive(ModelCondition.DOCKING, true);
        assertEquals(ModelCondition.FIRING, controller.currentCondition());

        controller.setActive(ModelCondition.DYING, true);
        assertEquals(ModelCondition.DYING, controller.currentCondition());
    }

    @Test
    void clearingConditionFallsBack() {
        AnimationController controller = new AnimationController();
        controller.setActive(ModelCondition.FIRING, true);
        controller.setActive(ModelCondition.MOVING, true);

        controller.setActive(ModelCondition.FIRING, false);
        assertEquals(ModelCondition.MOVING, controller.currentCondition());

        controller.clear();
        assertEquals(ModelCondition.IDLE, controller.currentCondition());
    }

    @Test
    void clipNameResolvesForWinningCondition() {
        AnimationController controller = new AnimationController();
        controller.setClipName(ModelCondition.FIRING, "fire");
        controller.setClipName(ModelCondition.MOVING, "walk");

        controller.setActive(ModelCondition.MOVING, true);
        controller.setActive(ModelCondition.FIRING, true);
        assertEquals("fire", controller.currentClipName().orElseThrow());
    }

    @Test
    void noClipBoundReturnsEmpty() {
        AnimationController controller = new AnimationController();
        controller.setActive(ModelCondition.DOCKING, true);
        assertTrue(controller.currentClipName().isEmpty());
    }
}
