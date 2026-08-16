package com.jrts.turret;

/**
 * Callback invoked when the turret state machine reaches the FIRE state and is
 * aligned with an in-range target.
 *
 * <p>In the full game this hook will delegate to the owning unit's weapon
 * system (which applies cooldown/reload and spawns the actual shot). In Stage
 * 1 there is no weapon system, so the listener is optional and simply signals
 * that the turret has fired.</p>
 */
@FunctionalInterface
public interface TurretFiringListener {

    /**
     * Called once each time the turret fires.
     */
    void onFire();
}
