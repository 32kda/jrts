package com.jrts.turret;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;

/**
 * Per-unit turret behaviour as a JME {@link AbstractControl}.
 *
 * <p>This is the JME-native, Composite-pattern equivalent of the old
 * {@code TurretController}: instead of Main looping over units and calling a
 * shared controller, each turret unit carries its own control. The control is
 * attached to the unit's spatial, so JME's scene-graph update traversal
 * ({@code Node.updateGeometricState}) invokes {@link #controlUpdate(float)}
 * automatically every frame, propagating the frame delta through the tree.</p>
 *
 * <p>The control owns a {@link TurretStateMachine} and drives the
 * TurretPivot (yaw) and BarrelPivot (pitch) nodes. The aim target is pushed in
 * each frame via {@link #setTarget(Vector3f)} / {@link #clearTarget()}.</p>
 */
public class TurretControl extends AbstractControl {

    private final TurretStateMachine machine;
    private final Node turretPivot;
    private final Node barrelPivot;
    private final float[] eulerAngles = new float[3];

    public TurretControl(TurretConfig config, Node turretPivot, Node barrelPivot) {
        this(config, turretPivot, barrelPivot, null);
    }

    public TurretControl(TurretConfig config, Node turretPivot, Node barrelPivot,
                         TurretFiringListener firingListener) {
        this.machine = new TurretStateMachine(config, firingListener);
        this.turretPivot = turretPivot;
        this.barrelPivot = barrelPivot;
    }

    /**
     * @return the underlying state machine (for inspection / weapon wiring)
     */
    public TurretStateMachine getStateMachine() {
        return machine;
    }

    /**
     * Assigns the world-space aim point for the next frame.
     */
    public void setTarget(Vector3f worldPoint) {
        machine.setTarget(worldPoint);
    }

    /**
     * Clears the current aim point; the turret holds briefly, then recenters.
     */
    public void clearTarget() {
        machine.clearTarget();
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (spatial == null || turretPivot == null) {
            return;
        }

        Vector3f position = spatial.getWorldTranslation();
        float bodyYaw = spatial.getLocalRotation().toAngles(eulerAngles)[1];

        machine.update(tpf, position, bodyYaw);
        applyRotation();
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        // Turret rotation is a scene-graph transform; nothing to render here.
    }

    private void applyRotation() {
        turretPivot.setLocalRotation(
                new Quaternion().fromAngleAxis(machine.getYaw(), Vector3f.UNIT_Y));

        if (barrelPivot != null && machine.allowsPitch()) {
            barrelPivot.setLocalRotation(
                    new Quaternion().fromAngleAxis(machine.getPitch(), Vector3f.UNIT_X));
        }
    }
}
