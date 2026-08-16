package com.jrts.building;

import com.jme3.asset.AssetManager;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;
import com.jme3.texture.Texture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Emits smoke from a building's {@code Smoke} empty node.
 *
 * <p>Smoke is emitted while the building is producing (factory chimney) or damaged (health
 * below a fraction of maximum). The actual emission uses a JME {@link ParticleEmitter} with a
 * shared smoke texture; if the texture is missing the control logs once and disables itself
 * rather than failing.</p>
 *
 * <p>The emission decision is a pure static method ({@link #shouldEmit}) so it can be unit
 * tested without a JME context.</p>
 */
public class BuildingSmokeControl extends AbstractControl {

    private static final Logger log = LoggerFactory.getLogger(BuildingSmokeControl.class);

    private static final String SMOKE_TEXTURE = "textures/fx/smoke.png";
    private static final float DEFAULT_DAMAGE_THRESHOLD = 0.5f;

    private final Building building;
    private final AssetManager assetManager;
    private final float damageThreshold;

    private boolean producing;
    private boolean textureMissing;
    private ParticleEmitter emitter;

    public BuildingSmokeControl(Building building, AssetManager assetManager) {
        this(building, assetManager, DEFAULT_DAMAGE_THRESHOLD);
    }

    public BuildingSmokeControl(Building building, AssetManager assetManager, float damageThreshold) {
        this.building = building;
        this.assetManager = assetManager;
        this.damageThreshold = damageThreshold;
    }

    /**
     * Set whether the building is currently producing (drives chimney smoke).
     */
    public void setProducing(boolean producing) {
        this.producing = producing;
    }

    /**
     * @return true if smoke should be emitted, given the building's state
     */
    public static boolean shouldEmit(boolean hasSmokePoint, boolean producing,
                                     float healthFraction, float damageThreshold) {
        return hasSmokePoint && (producing || healthFraction < damageThreshold);
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (spatial == null) {
            return;
        }
        float maxHealth = building.getConfig().stats().health();
        float fraction = maxHealth <= 0f ? 1f : building.getHealth() / (float) maxHealth;
        boolean emit = shouldEmit(building.getSmokePoint() != null, producing,
                fraction, damageThreshold);

        if (emit && emitter == null && !textureMissing) {
            emitter = createEmitter();
        }
        if (emit && emitter != null && emitter.getParent() == null) {
            building.getSmokePoint().attachChild(emitter);
        } else if (!emit && emitter != null && emitter.getParent() != null) {
            emitter.removeFromParent();
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        // Particles render through JME's own traversal.
    }

    private ParticleEmitter createEmitter() {
        try {
            Texture texture = assetManager.loadTexture(SMOKE_TEXTURE);
            ParticleEmitter emitter = new ParticleEmitter("BuildingSmoke",
                    ParticleMesh.Type.Triangle, 30);
            emitter.setLocalTranslation(0f, 0f, 0f);
            emitter.setStartColor(new ColorRGBA(0.4f, 0.4f, 0.4f, 0.6f));
            emitter.setEndColor(new ColorRGBA(0.6f, 0.6f, 0.6f, 0f));
            emitter.setStartSize(0.5f);
            emitter.setEndSize(2f);
            emitter.setLowLife(1.5f);
            emitter.setHighLife(3f);
            emitter.setParticlesPerSec(10f);
            emitter.getParticleInfluencer()
                    .setInitialVelocity(new com.jme3.math.Vector3f(0f, 1.5f, 0f));
            Material material = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
            material.setTexture("Texture", texture);
            emitter.setMaterial(material);
            return emitter;
        } catch (Exception e) {
            log.warn("Smoke texture '{}' unavailable; disabling building smoke: {}",
                    SMOKE_TEXTURE, e.getMessage());
            textureMissing = true;
            return null;
        }
    }
}
