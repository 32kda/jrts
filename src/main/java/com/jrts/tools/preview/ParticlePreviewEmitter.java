package com.jrts.tools.preview;

import com.jme3.asset.AssetManager;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

/**
 * Emits muzzle flash particles for preview testing.
 * One-shot burst triggered by button press.
 */
public class ParticlePreviewEmitter {

    private final Node rootNode;
    private Node muzzleNode;
    private ParticleEmitter emitter;

    public ParticlePreviewEmitter(Node rootNode, AssetManager assetManager) {
        this.rootNode = rootNode;
    }

    public void setMuzzleNode(Node muzzleNode) {
        this.muzzleNode = muzzleNode;
    }

    public void emitBurst() {
        if (muzzleNode == null) {
            return;
        }

        if (emitter != null) {
            emitter.removeFromParent();
        }

        emitter = new ParticleEmitter("MuzzleFlash", ParticleMesh.Type.Triangle, 30);
        emitter.setStartColor(new ColorRGBA(1f, 0.8f, 0.2f, 1f));
        emitter.setEndColor(new ColorRGBA(1f, 0.2f, 0f, 0f));
        emitter.setStartSize(0.3f);
        emitter.setEndSize(0.1f);
        emitter.setGravity(0, 0, 0);
        emitter.setLowLife(0.1f);
        emitter.setHighLife(0.3f);
        emitter.setParticlesPerSec(0);
        emitter.getParticleInfluencer().setInitialVelocity(
                new Vector3f(0, 2f, 5f));
        emitter.getParticleInfluencer().setVelocityVariation(0.5f);
        emitter.setImagesX(1);
        emitter.setImagesY(1);

        Vector3f muzzleWorldPos = muzzleNode.getWorldTranslation();
        emitter.setLocalTranslation(muzzleWorldPos);
        emitter.emitAllParticles();

        rootNode.attachChild(emitter);
    }

    public void setEnabled(boolean enabled) {
        if (emitter != null) {
            emitter.setEnabled(enabled);
        }
    }
}
