package com.jrts.movement;

import com.jrts.scene.TerrainHeightProvider;
import com.jrts.unit.Unit;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

/**
 * Per-unit movement as a JME {@link AbstractControl}.
 *
 * <p>This is the JME-native, Composite-pattern equivalent of driving {@link MovementController}
 * from a central loop: each movable unit carries its own control, attached to its spatial, so
 * JME's scene-graph update traversal invokes {@link #controlUpdate(float)} every frame.</p>
 *
 * <p>Delegates the movement math to the engine-independent {@link MovementController}, keeping
 * the logic testable without a JME context.</p>
 */
public class MovementControl extends AbstractControl {

    private final Unit unit;
    private final MovementController controller;

    public MovementControl(Unit unit, TerrainHeightProvider terrain) {
        this.unit = unit;
        this.controller = new MovementController(terrain);
    }

    @Override
    protected void controlUpdate(float tpf) {
        controller.update(unit, tpf);
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        // Movement is a scene-graph transform; nothing to render here.
    }
}
