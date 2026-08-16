package com.jrts.turret;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TurretStateMachineTest {

    private static final float DELTA = 1e-4f;

    @Test
    void setTargetTransitionsIdleToAim() {
        TurretStateMachine machine = new TurretStateMachine(defaultConfig());

        assertEquals(TurretState.IDLE, machine.getState());

        machine.setTarget(new Vector3f(10, 0, 0));

        assertEquals(TurretState.AIM, machine.getState());
    }

    @Test
    void yawStepsTowardTargetOverFrames() {
        TurretStateMachine machine = new TurretStateMachine(configWithRates(1f, 1f, false));
        machine.setTarget(new Vector3f(10, 0, 0));

        machine.update(0.1f, Vector3f.ZERO, 0f);

        assertEquals(TurretState.AIM, machine.getState());
        assertTrue(machine.getYaw() > 0f, "yaw should advance toward target");
        assertTrue(machine.getYaw() < FastMath.HALF_PI, "yaw should not overshoot");
    }

    @Test
    void alignedTurretFiresThenReturnsToAim() {
        AtomicInteger shots = new AtomicInteger();
        TurretStateMachine machine = new TurretStateMachine(defaultConfig(), shots::incrementAndGet);

        machine.setTarget(new Vector3f(10, 0, 0));
        machine.update(1f, Vector3f.ZERO, 0f);

        assertEquals(TurretState.FIRE, machine.getState());
        assertEquals(0, shots.get(), "fire is deferred to the FIRE state update");

        machine.update(1f, Vector3f.ZERO, 0f);

        assertEquals(1, shots.get(), "one shot fired");
        assertEquals(TurretState.AIM, machine.getState());
    }

    @Test
    void pitchRisesTowardElevatedTarget() {
        TurretStateMachine machine = new TurretStateMachine(configWithRates(1f, 1f, true));
        machine.setTarget(new Vector3f(10, 10, 10));

        machine.update(1f, Vector3f.ZERO, 0f);

        // Elevation = atan2(10, sqrt(10^2+10^2)) ≈ 35.26 degrees.
        float expected = (float) Math.atan2(10f, Math.sqrt(200f));
        assertEquals(expected, machine.getPitch(), DELTA);
    }

    @Test
    void pitchIsClampedToMaxPitch() {
        TurretStateMachine machine = new TurretStateMachine(
                pitchConfig(true, 0f, -5f, 20f));
        machine.setTarget(new Vector3f(10, 10, 10));

        machine.update(1f, Vector3f.ZERO, 0f);

        assertEquals(FastMath.DEG_TO_RAD * 20f, machine.getPitch(), DELTA);
    }

    @Test
    void fixedFirePitchOverridesAimElevation() {
        TurretStateMachine machine = new TurretStateMachine(
                pitchConfig(true, 30f, -5f, 45f));
        machine.setTarget(new Vector3f(10, 10, 10));

        machine.update(1f, Vector3f.ZERO, 0f);

        assertEquals(FastMath.DEG_TO_RAD * 30f, machine.getPitch(), DELTA);
    }

    @Test
    void nonPitchTurretKeepsNaturalPitch() {
        TurretStateMachine machine = new TurretStateMachine(
                pitchConfig(false, 0f, -5f, 45f));
        machine.setTarget(new Vector3f(10, 10, 10));

        machine.update(1f, Vector3f.ZERO, 0f);

        assertEquals(0f, machine.getPitch(), DELTA);
    }

    @Test
    void losingTargetRecoversToIdle() {
        TurretStateMachine machine = new TurretStateMachine(defaultConfig());

        machine.setTarget(new Vector3f(10, 0, 0));
        machine.update(1f, Vector3f.ZERO, 0f);
        machine.clearTarget();

        assertEquals(TurretState.HOLD, machine.getState());

        machine.update(2f, Vector3f.ZERO, 0f);
        assertEquals(TurretState.RECENTER, machine.getState());

        machine.update(1f, Vector3f.ZERO, 0f);
        assertEquals(TurretState.IDLE, machine.getState());
        assertEquals(0f, machine.getYaw(), DELTA);
    }

    @Test
    void idleScanOccursAfterInterval() {
        TurretStateMachine machine = new TurretStateMachine(scanConfig(-15f, 15f, 1f));

        machine.update(1.1f, Vector3f.ZERO, 0f);
        assertEquals(TurretState.IDLE_SCAN, machine.getState());

        machine.update(1f, Vector3f.ZERO, 0f);
        assertEquals(TurretState.IDLE, machine.getState());
    }

    @Test
    void noIdleScanWhenNotConfigured() {
        TurretStateMachine machine = new TurretStateMachine(defaultConfig());

        machine.update(100f, Vector3f.ZERO, 0f);

        assertEquals(TurretState.IDLE, machine.getState());
    }

    private static TurretConfig defaultConfig() {
        return configWithRates(FastMath.TWO_PI, FastMath.TWO_PI, false);
    }

    private static TurretConfig configWithRates(float turnRate, float pitchRate, boolean allowsPitch) {
        return new TurretConfig(turnRate, pitchRate, allowsPitch,
                0f, 0f, 0f, -FastMath.DEG_TO_RAD * 5f, FastMath.DEG_TO_RAD * 45f,
                0f, 0f, 5f, 2f);
    }

    private static TurretConfig pitchConfig(boolean allowsPitch, float firePitchDeg,
                                            float minPitchDeg, float maxPitchDeg) {
        return new TurretConfig(1f, 1f, allowsPitch,
                0f, 0f, FastMath.DEG_TO_RAD * firePitchDeg,
                FastMath.DEG_TO_RAD * minPitchDeg, FastMath.DEG_TO_RAD * maxPitchDeg,
                0f, 0f, 5f, 2f);
    }

    private static TurretConfig scanConfig(float minScanDeg, float maxScanDeg, float interval) {
        return new TurretConfig(1f, 1f, false,
                0f, 0f, 0f, -FastMath.DEG_TO_RAD * 5f, FastMath.DEG_TO_RAD * 45f,
                FastMath.DEG_TO_RAD * minScanDeg, FastMath.DEG_TO_RAD * maxScanDeg,
                interval, 2f);
    }
}
