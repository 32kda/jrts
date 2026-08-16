package com.jrts.docking;

/**
 * Heals the docked target over time, up to its maximum health (a repair-pad / barracks dock).
 */
public class RepairDockAction implements DockAction {

    private final int healPerSecond;

    public RepairDockAction(int healPerSecond) {
        this.healPerSecond = healPerSecond;
    }

    @Override
    public void apply(DockTarget target, float tpf) {
        int next = Math.min(target.getMaxHealth(),
                target.getHealth() + (int) Math.ceil(healPerSecond * tpf));
        target.setHealth(next);
    }
}
