package com.jrts.tools.preview;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

/**
 * Rotates the TurretPivot Node (local Y axis → yaw) and
 * BarrelPivot Node (local X axis → pitch) in response to
 * UI slider values or auto-scan mode.
 */
public class TurretPreviewController {

    private Node turretPivot;
    private Node barrelPivot;

    private float targetYawDeg;
    private float targetPitchDeg;
    private float currentYawRad;
    private float currentPitchRad;

    private boolean autoScan;
    private float scanAngle;
    private float scanDirection = 1f;
    private final float minScanAngle = -45f * FastMath.DEG_TO_RAD;
    private final float maxScanAngle = 45f * FastMath.DEG_TO_RAD;
    private final float scanSpeed = 0.5f;

    public TurretPreviewController() {
    }

    public void setModels(Node turretPivot, Node barrelPivot) {
        this.turretPivot = turretPivot;
        this.barrelPivot = barrelPivot;
    }

    public void setYawDegrees(float degrees) {
        targetYawDeg = degrees;
    }

    public void setPitchDegrees(float degrees) {
        targetPitchDeg = degrees;
    }

    public void setAutoScan(boolean enabled) {
        this.autoScan = enabled;
        if (enabled) {
            scanAngle = 0;
            scanDirection = 1f;
        }
    }

    public boolean isAutoScan() {
        return autoScan;
    }

    public void update(float tpf) {
        if (turretPivot == null) {
            return;
        }

        if (autoScan) {
            scanAngle += scanSpeed * scanDirection * tpf;
            if (scanAngle > maxScanAngle) {
                scanAngle = maxScanAngle;
                scanDirection = -1f;
            } else if (scanAngle < minScanAngle) {
                scanAngle = minScanAngle;
                scanDirection = 1f;
            }
            targetYawDeg = scanAngle * FastMath.RAD_TO_DEG;
        }

        float targetYaw = targetYawDeg * FastMath.DEG_TO_RAD;
        float smoothRate = 5f;
        currentYawRad += (targetYaw - currentYawRad) * smoothRate * tpf;
        turretPivot.setLocalRotation(
                new Quaternion().fromAngleAxis(currentYawRad, Vector3f.UNIT_Y));

        if (barrelPivot != null) {
            float targetPitch = targetPitchDeg * FastMath.DEG_TO_RAD;
            currentPitchRad += (targetPitch - currentPitchRad) * smoothRate * tpf;
            barrelPivot.setLocalRotation(
                    new Quaternion().fromAngleAxis(currentPitchRad, Vector3f.UNIT_X));
        }
    }

    public float getCurrentYawDeg() {
        return currentYawRad * FastMath.RAD_TO_DEG;
    }

    public float getCurrentPitchDeg() {
        return currentPitchRad * FastMath.RAD_TO_DEG;
    }
}
