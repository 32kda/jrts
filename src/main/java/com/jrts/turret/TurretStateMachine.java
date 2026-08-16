package com.jrts.turret;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

import java.util.Random;

/**
 * Pure-logic finite state machine for a single turret, mirroring the spec's
 * TurretAI state machine (IDLE / IDLE_SCAN / AIM / FIRE / HOLD / RECENTER).
 *
 * <p>This class owns the turret's logical yaw and pitch (in radians, relative
 * to the owning unit's body) and advances them over time using the resolved
 * {@link TurretConfig}. It has no dependency on the JME scene graph;
 * {@link TurretControl} reads {@link #getYaw()} / {@link #getPitch()} each
 * frame and writes them to the TurretPivot / BarrelPivot nodes.</p>
 *
 * <p>{@link #update(float, Vector3f, float)} receives the owner's world
 * position and body yaw so the desired turret yaw can be computed relative to
 * the body.</p>
 */
public class TurretStateMachine {

    /** Relative-angle tolerance below which the turret is considered aligned (~2 deg). */
    private static final float ALIGN_THRESHOLD = 0.035f;
    /** Rate modifier used while recentering or idle-scanning (half speed). */
    private static final float SLOW_RATE_MODIFIER = 0.5f;

    private final TurretConfig config;
    private final TurretFiringListener firingListener;
    private final Random random;

    private TurretState state = TurretState.IDLE;
    private float yaw;
    private float pitch;

    private boolean hasTarget;
    private final Vector3f targetPoint = new Vector3f();

    private float idleScanTimer;
    private float idleScanOffset;
    private float holdTimer;

    public TurretStateMachine(TurretConfig config) {
        this(config, null);
    }

    public TurretStateMachine(TurretConfig config, TurretFiringListener firingListener) {
        this(config, firingListener, new Random());
    }

    public TurretStateMachine(TurretConfig config, TurretFiringListener firingListener, Random random) {
        this.config = config;
        this.firingListener = firingListener;
        this.random = random;
        this.yaw = config.naturalTurretAngle();
        this.pitch = config.naturalTurretPitch();
        this.idleScanTimer = config.idleScanInterval();
    }

    public TurretState getState() {
        return state;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public boolean hasTarget() {
        return hasTarget;
    }

    public boolean allowsPitch() {
        return config.allowsPitch();
    }

    /**
     * Assigns a world-space aim point, entering the AIM state if not already
     * tracking. A {@code null} point clears the target instead.
     */
    public void setTarget(Vector3f worldPoint) {
        if (worldPoint == null) {
            clearTarget();
            return;
        }
        targetPoint.set(worldPoint);
        if (!hasTarget) {
            hasTarget = true;
            transitionTo(TurretState.AIM);
        }
    }

    /**
     * Clears the current aim point. The turret holds briefly (HOLD), then
     * recenters (RECENTER).
     */
    public void clearTarget() {
        if (hasTarget) {
            hasTarget = false;
            transitionTo(TurretState.HOLD);
        }
    }

    /**
     * Advances the state machine by one frame.
     *
     * @param tpf          time per frame, seconds
     * @param unitPosition owner's world position
     * @param unitYaw      owner's body yaw, radians
     */
    public void update(float tpf, Vector3f unitPosition, float unitYaw) {
        switch (state) {
            case IDLE -> updateIdle(tpf, unitPosition, unitYaw);
            case IDLE_SCAN -> updateIdleScan(tpf, unitPosition, unitYaw);
            case AIM -> updateAim(tpf, unitPosition, unitYaw);
            case FIRE -> updateFire();
            case HOLD -> updateHold(tpf, unitPosition, unitYaw);
            case RECENTER -> updateRecenter(tpf, unitPosition, unitYaw);
        }
    }

    private void updateIdle(float tpf, Vector3f unitPosition, float unitYaw) {
        if (hasTarget) {
            transitionTo(TurretState.AIM);
            updateAim(tpf, unitPosition, unitYaw);
            return;
        }
        if (isIdleScanConfigured()) {
            idleScanTimer -= tpf;
            if (idleScanTimer <= 0f) {
                transitionTo(TurretState.IDLE_SCAN);
            }
        }
    }

    private void updateIdleScan(float tpf, Vector3f unitPosition, float unitYaw) {
        if (hasTarget) {
            transitionTo(TurretState.AIM);
            updateAim(tpf, unitPosition, unitYaw);
            return;
        }
        boolean yawAligned = turnTowardsAngle(
                config.naturalTurretAngle() + idleScanOffset, tpf, SLOW_RATE_MODIFIER);
        boolean pitchAligned = turnTowardsPitch(config.naturalTurretPitch(), tpf, SLOW_RATE_MODIFIER);
        if (yawAligned && pitchAligned) {
            transitionTo(TurretState.IDLE);
        }
    }

    private void updateAim(float tpf, Vector3f unitPosition, float unitYaw) {
        if (!hasTarget) {
            transitionTo(TurretState.HOLD);
            updateHold(tpf, unitPosition, unitYaw);
            return;
        }

        boolean yawAligned = turnTowardsAngle(desiredYaw(unitPosition, unitYaw), tpf, 1f);
        boolean pitchAligned = true;
        if (config.allowsPitch()) {
            pitchAligned = turnTowardsPitch(desiredPitch(unitPosition), tpf, 1f);
        }

        if (yawAligned && pitchAligned) {
            transitionTo(TurretState.FIRE);
        }
    }

    private void updateFire() {
        if (firingListener != null) {
            firingListener.onFire();
        }
        // Stage 1 has no weapon system, so the shot completes instantly and we
        // return to AIM (or HOLD if the target vanished mid-frame). Real
        // weapon gating (cooldown/reload) belongs to the weapon state machine.
        transitionTo(hasTarget ? TurretState.AIM : TurretState.HOLD);
    }

    private void updateHold(float tpf, Vector3f unitPosition, float unitYaw) {
        if (hasTarget) {
            transitionTo(TurretState.AIM);
            updateAim(tpf, unitPosition, unitYaw);
            return;
        }
        holdTimer -= tpf;
        if (holdTimer <= 0f) {
            transitionTo(TurretState.RECENTER);
        }
    }

    private void updateRecenter(float tpf, Vector3f unitPosition, float unitYaw) {
        if (hasTarget) {
            transitionTo(TurretState.AIM);
            updateAim(tpf, unitPosition, unitYaw);
            return;
        }
        boolean yawAligned = turnTowardsAngle(config.naturalTurretAngle(), tpf, SLOW_RATE_MODIFIER);
        boolean pitchAligned = turnTowardsPitch(config.naturalTurretPitch(), tpf, SLOW_RATE_MODIFIER);
        if (yawAligned && pitchAligned) {
            transitionTo(TurretState.IDLE);
        }
    }

    private float desiredYaw(Vector3f unitPosition, float unitYaw) {
        float dx = targetPoint.x - unitPosition.x;
        float dz = targetPoint.z - unitPosition.z;
        return normalizeAngle((float) Math.atan2(dx, dz) - unitYaw);
    }

    private float desiredPitch(Vector3f unitPosition) {
        if (config.firePitch() > 0f) {
            return config.firePitch();
        }
        float dx = targetPoint.x - unitPosition.x;
        float dy = targetPoint.y - unitPosition.y;
        float dz = targetPoint.z - unitPosition.z;
        float horizontal = (float) Math.sqrt(dx * dx + dz * dz);
        float elevation = (float) Math.atan2(dy, horizontal);
        return clamp(elevation, config.minPitch(), config.maxPitch());
    }

    /**
     * Rotates the turret yaw toward the desired angle at the configured turn
     * rate, snapping when one step would overshoot.
     *
     * @return true when aligned within {@link #ALIGN_THRESHOLD}
     */
    private boolean turnTowardsAngle(float desiredAngle, float tpf, float rateModifier) {
        desiredAngle = normalizeAngle(desiredAngle);
        float diff = normalizeAngle(desiredAngle - yaw);
        float maxStep = config.turnRate() * rateModifier * tpf;

        if (Math.abs(diff) < maxStep) {
            yaw = desiredAngle;
            return true;
        }
        yaw = normalizeAngle(yaw + Math.signum(diff) * maxStep);
        return Math.abs(normalizeAngle(desiredAngle - yaw)) <= ALIGN_THRESHOLD;
    }

    /**
     * Rotates the barrel pitch toward the desired angle at the configured
     * pitch rate. Returns immediately-aligned when pitch is not allowed.
     */
    private boolean turnTowardsPitch(float desiredPitch, float tpf, float rateModifier) {
        if (!config.allowsPitch()) {
            return true;
        }
        float diff = desiredPitch - pitch;
        float maxStep = config.pitchRate() * rateModifier * tpf;

        if (Math.abs(diff) < maxStep) {
            pitch = desiredPitch;
            return true;
        }
        pitch += Math.signum(diff) * maxStep;
        return Math.abs(desiredPitch - pitch) <= ALIGN_THRESHOLD;
    }

    private boolean isIdleScanConfigured() {
        return config.minIdleScanAngle() != 0f || config.maxIdleScanAngle() != 0f;
    }

    private void beginIdleScan() {
        float minA = config.minIdleScanAngle();
        float maxA = config.maxIdleScanAngle();
        if (minA == 0f && maxA == 0f) {
            idleScanOffset = 0f;
            return;
        }
        idleScanOffset = minA + random.nextFloat() * (maxA - minA);
        if (random.nextBoolean()) {
            idleScanOffset = -idleScanOffset;
        }
    }

    private void transitionTo(TurretState next) {
        if (state == next) {
            return;
        }
        state = next;
        switch (next) {
            case IDLE -> idleScanTimer = config.idleScanInterval();
            case IDLE_SCAN -> beginIdleScan();
            case HOLD -> holdTimer = config.recenterTime();
            default -> {
                // AIM / FIRE / RECENTER need no per-state setup.
            }
        }
    }

    private static float normalizeAngle(float rad) {
        while (rad > FastMath.PI) {
            rad -= FastMath.TWO_PI;
        }
        while (rad < -FastMath.PI) {
            rad += FastMath.TWO_PI;
        }
        return rad;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
