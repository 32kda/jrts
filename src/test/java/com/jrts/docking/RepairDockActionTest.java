package com.jrts.docking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RepairDockActionTest {

    @Test
    void healsTowardMax() {
        DockTarget target = target(20, 100);
        new RepairDockAction(10).apply(target, 2f);
        assertEquals(40, target.getHealth());
    }

    @Test
    void healingClampedToMax() {
        DockTarget target = target(20, 100);
        new RepairDockAction(1000).apply(target, 1f);
        assertEquals(100, target.getHealth());
    }

    private static DockTarget target(int initialHealth, int maxHealth) {
        return new DockTarget() {
            private int health = initialHealth;

            @Override
            public int getHealth() {
                return health;
            }

            @Override
            public void setHealth(int health) {
                this.health = health;
            }

            @Override
            public int getMaxHealth() {
                return maxHealth;
            }
        };
    }
}
