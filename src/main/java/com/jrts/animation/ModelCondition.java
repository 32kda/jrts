package com.jrts.animation;

/**
 * Animation conditions that map runtime state to a named animation clip, mirroring the
 * spec's {@code MODELCONDITION} flags (section 2.3). Each condition carries a priority so
 * that when several are active at once the {@link AnimationController} can pick the
 * highest-priority one (e.g. DYING beats FIRING beats MOVING).
 */
public enum ModelCondition {

    IDLE(0),
    GARRISONED(1),
    DEPLOYED(2),
    MOVING(3),
    DOCKING(4),
    BUILDING(5),
    RELOADING(6),
    PREATTACK(7),
    FIRING(8),
    AFLAME(9),
    DYING(10);

    private final int priority;

    ModelCondition(int priority) {
        this.priority = priority;
    }

    /**
     * @return priority used to resolve conflicting simultaneous conditions
     */
    public int priority() {
        return priority;
    }
}
