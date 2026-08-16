package com.jrts.docking;

/**
 * Action executed while a unit is docked (spec section 8.2): heal, repair, reload, unload,
 * transfer supply, etc.
 */
@FunctionalInterface
public interface DockAction {

    /**
     * Apply this action's effect to the docked target for one frame.
     *
     * @param target the docked unit
     * @param tpf    time per frame, seconds
     */
    void apply(DockTarget target, float tpf);
}
