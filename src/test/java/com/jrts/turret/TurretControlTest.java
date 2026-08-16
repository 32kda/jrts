package com.jrts.turret;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TurretControlTest {

    private static final float TPF = 1f / 60f;

    @Test
    void controlRotatesTurretTowardTarget() {
        Node turretPivot = new Node("TurretPivot");
        TurretControl control = newControl(turretPivot, null, false);

        control.setTarget(new Vector3f(10, 0, 0));
        control.update(TPF);

        float[] angles = turretPivot.getLocalRotation().toAngles(null);
        assertTrue(angles[1] > 0, "turret yaw should advance toward target");
    }

    @Test
    void controlPitchesBarrelTowardElevatedTarget() {
        Node turretPivot = new Node("TurretPivot");
        Node barrelPivot = new Node("BarrelPivot");
        TurretControl control = newControl(turretPivot, barrelPivot, true);

        control.setTarget(new Vector3f(0, 10, 10));
        control.update(TPF);

        float[] angles = barrelPivot.getLocalRotation().toAngles(null);
        assertTrue(angles[0] > 0, "barrel should pitch upward toward elevated target");
    }

    @Test
    void controlLeavesBarrelUntouchedWhenPitchDisabled() {
        Node turretPivot = new Node("TurretPivot");
        Node barrelPivot = new Node("BarrelPivot");
        TurretControl control = newControl(turretPivot, barrelPivot, false);

        control.setTarget(new Vector3f(0, 10, 10));
        control.update(TPF);

        float[] angles = barrelPivot.getLocalRotation().toAngles(null);
        assertEquals(0f, angles[0], 1e-6f);
        assertEquals(0f, angles[1], 1e-6f);
        assertEquals(0f, angles[2], 1e-6f);
    }

    @Test
    void setTargetMovesStateMachineToAim() {
        TurretControl control = newControl(new Node("TurretPivot"), null, false);

        control.setTarget(new Vector3f(10, 0, 0));

        assertEquals(TurretState.AIM, control.getStateMachine().getState());
    }

    private static TurretControl newControl(Node turretPivot, Node barrelPivot, boolean allowsPitch) {
        Node spatial = new Node("Spatial");
        spatial.attachChild(turretPivot);
        if (barrelPivot != null) {
            spatial.attachChild(barrelPivot);
        }

        TurretConfig config = config(allowsPitch);
        TurretControl control = new TurretControl(config, turretPivot, barrelPivot);
        spatial.addControl(control);
        return control;
    }

    private static TurretConfig config(boolean allowsPitch) {
        return new TurretConfig(
                FastMath.TWO_PI,
                FastMath.TWO_PI,
                allowsPitch,
                0f, 0f, 0f,
                -FastMath.DEG_TO_RAD * 5f, FastMath.DEG_TO_RAD * 45f,
                0f, 0f, 5f, 2f);
    }
}
