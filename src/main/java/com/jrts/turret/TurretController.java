package com.jrts.turret;

import com.jrts.unit.Unit;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rotates a unit's TurretPivot node toward a world-space target point.
 *
 * Stage 1: yaw-only rotation (turret tracks cursor on terrain).
 * Pitch (barrel elevation) deferred to Stage 2+ when weapons are implemented.
 *
 * Called each frame for each selected unit that has a turret.
 * If no explicit target, turret returns to natural angle (idle).
 */
public class TurretController {

    private static final Logger log = LoggerFactory.getLogger(TurretController.class);

    /**
     * @param unit        unit with turret
     * @param targetPoint world position to aim at (from mouse-pick on terrain)
     * @param tpf         time per frame
     */
    public void update(Unit unit, Vector3f targetPoint, float tpf) {
        if (!unit.hasTurret() || unit.getTurretPivot() == null) {
            return;
        }

        Node turretPivot = unit.getTurretPivot();
        Vector3f unitPos = unit.getPosition();
        Vector3f toTarget = targetPoint.subtract(unitPos);

        float desiredTurretYaw = (float) Math.atan2(toTarget.x, toTarget.z);

        float[] angles = turretPivot.getLocalRotation().toAngles(null);
        float currentYaw = angles[1];

        float yawDiff = normalizeAngle(desiredTurretYaw - currentYaw);
        float turnRate = unit.getConfig().turrets().turretRotationSpeed() * FastMath.DEG_TO_RAD;
        float step = Math.min(Math.abs(yawDiff), turnRate * tpf);
        float newYaw = currentYaw + Math.signum(yawDiff) * step;

        turretPivot.setLocalRotation(new Quaternion().fromAngleAxis(newYaw, Vector3f.UNIT_Y));
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
}
