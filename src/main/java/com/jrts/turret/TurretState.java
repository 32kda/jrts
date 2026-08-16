package com.jrts.turret;

/**
 * States of the turret finite state machine, mirroring the spec's
 * TurretStateMachine (section 7.3):
 *
 * <pre>
 * IDLE ──scan_timeout──▶ IDLE_SCAN ──scan_complete──▶ IDLE
 * IDLE ──target_set───▶ AIM ──aimed&amp;&amp;in_range──▶ FIRE ──▶ AIM / HOLD
 * HOLD ──timeout──────▶ RECENTER ──centered──▶ IDLE
 * </pre>
 */
public enum TurretState {
    /** Resting at (or returning to) the natural angle, waiting for work. */
    IDLE,
    /** Rotating to a random offset near the natural angle to appear alert. */
    IDLE_SCAN,
    /** Rotating yaw (and pitch, if allowed) toward the current target. */
    AIM,
    /** Aligned and within range; weapon fires, then returns to AIM. */
    FIRE,
    /** Holding position briefly after losing a target before recentering. */
    HOLD,
    /** Rotating back to the natural angle/pitch at reduced speed. */
    RECENTER
}
