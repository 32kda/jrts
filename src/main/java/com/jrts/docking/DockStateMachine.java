package com.jrts.docking;

/**
 * Engine-independent dock cycle state machine.
 *
 * <p>Sequence: {@code IDLE → APPROACH → ENTER → DOCKED → EXIT → IDLE}. While
 * {@code DOCKED}, the machine counts down {@code dockDuration}; once it elapses the cycle
 * transitions to {@code EXIT} (the action is considered complete). Movement between the
 * approach/enter/dock/exit world positions is handled by the JME layer, which calls the
 * {@code onXxx()} events as the docking unit reaches each point.</p>
 */
public class DockStateMachine {

    private final float dockDuration;

    private DockState state = DockState.IDLE;
    private float dockTimer;

    public DockStateMachine(float dockDuration) {
        if (dockDuration < 0f) {
            throw new IllegalArgumentException("dockDuration must be >= 0");
        }
        this.dockDuration = dockDuration;
    }

    public DockState state() {
        return state;
    }

    /**
     * @return true while a dock cycle is in progress (anything but IDLE)
     */
    public boolean isBusy() {
        return state != DockState.IDLE;
    }

    /**
     * Begin a dock cycle. Ignored if already busy.
     */
    public void requestDock() {
        if (state == DockState.IDLE) {
            state = DockState.APPROACH;
        }
    }

    /**
     * Signal that the unit reached the approach position.
     */
    public void onApproachReached() {
        if (state == DockState.APPROACH) {
            state = DockState.ENTER;
        }
    }

    /**
     * Signal that the unit entered the dock.
     */
    public void onEntered() {
        if (state == DockState.ENTER) {
            state = DockState.DOCKED;
            dockTimer = dockDuration;
        }
    }

    /**
     * Advance the docked timer. When the action time elapses, transitions to EXIT.
     */
    public void update(float tpf) {
        if (state == DockState.DOCKED) {
            dockTimer -= tpf;
            if (dockTimer <= 0f) {
                state = DockState.EXIT;
            }
        }
    }

    /**
     * Signal that the unit exited; the cycle completes.
     */
    public void onExited() {
        if (state == DockState.EXIT) {
            state = DockState.IDLE;
        }
    }

    /**
     * Abort the current cycle immediately.
     */
    public void reset() {
        state = DockState.IDLE;
        dockTimer = 0f;
    }
}
