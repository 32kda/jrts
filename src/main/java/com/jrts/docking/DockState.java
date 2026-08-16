package com.jrts.docking;

/**
 * Phases of a dock cycle, mirroring the spec's simplified 4-phase dock protocol
 * (section 8.1): approach, enter, docked (action executes), exit.
 */
public enum DockState {
    IDLE,
    APPROACH,
    ENTER,
    DOCKED,
    EXIT
}
