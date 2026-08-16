package com.jrts.building;

import com.jrts.animation.AnimationController;
import com.jrts.animation.ModelCondition;
import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drives a building's bone animation from its {@link AnimationController}.
 *
 * <p>Each frame it reads the winning condition's clip name and, if the spatial carries an
 * {@link AnimControl}, crossfades to that clip. Buildings without an armature (or without a
 * bound clip for the winning condition) are left untouched, so this control is a safe no-op
 * for placeholder models.</p>
 *
 * <p>Typical building conditions: {@code BUILDING} (construction), {@code DOCKING} (gate
 * open/close), {@code FIRING} (defense turret), {@code AFLAME} (damaged), {@code DYING}.</p>
 */
@SuppressWarnings("deprecation")
public class BuildingAnimationControl extends AbstractControl {

    private static final Logger log = LoggerFactory.getLogger(BuildingAnimationControl.class);

    private final AnimationController controller;
    private String playingClip;

    public BuildingAnimationControl(AnimationController controller) {
        this.controller = controller;
    }

    public AnimationController getController() {
        return controller;
    }

    /**
     * Convenience: set a clip binding on the underlying controller.
     */
    public void bindClip(ModelCondition condition, String clipName) {
        controller.setClipName(condition, clipName);
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (spatial == null) {
            return;
        }
        AnimControl anim = spatial.getControl(AnimControl.class);
        if (anim == null) {
            return;
        }
        String clip = controller.currentClipName().orElse(null);
        if (clip == null || clip.equals(playingClip)) {
            return;
        }
        AnimChannel channel = anim.getChannel(0);
        channel.setAnim(clip);
        playingClip = clip;
        log.debug("Building animation -> '{}'", clip);
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        // Animations are applied by JME's own traversal; nothing to render here.
    }
}
