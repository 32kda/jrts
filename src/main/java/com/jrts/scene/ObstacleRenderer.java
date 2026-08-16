package com.jrts.scene;

import com.jrts.pathfinding.Obstacle;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Renders the visual representation of {@link Obstacle} footprints as JME primitives.
 *
 * Pure presentation concern: the obstacles' pathfinding footprint is already baked into the
 * grid (by the map); this class only adds the matching visible geometry so the player can see
 * what units path around.
 */
public class ObstacleRenderer {

    private static final Logger log = LoggerFactory.getLogger(ObstacleRenderer.class);

    private static final float HEIGHT = 5f;
    private static final ColorRGBA COLOR = new ColorRGBA(0.35f, 0.4f, 0.55f, 1f);
    private static final ColorRGBA AMBIENT = new ColorRGBA(0.15f, 0.18f, 0.25f, 1f);

    private final Node sceneRoot;
    private final AssetManager assetManager;

    public ObstacleRenderer(Node sceneRoot, AssetManager assetManager) {
        this.sceneRoot = sceneRoot;
        this.assetManager = assetManager;
    }

    public void render(List<Obstacle> obstacles) {
        for (Obstacle obstacle : obstacles) {
            render(obstacle);
        }
        log.info("Rendered {} obstacles", obstacles.size());
    }

    private void render(Obstacle obstacle) {
        switch (obstacle.getShape()) {
            case BOX -> renderBox(obstacle.getCenterX(), obstacle.getCenterZ(),
                    obstacle.getHalfX(), obstacle.getHalfZ(), obstacle.getYaw());
            case CYLINDER -> renderCylinder(obstacle.getCenterX(), obstacle.getCenterZ(),
                    obstacle.getRadius());
        }
    }

    private void renderBox(float cx, float cz, float halfX, float halfZ, float yaw) {
        Box box = new Box(halfX, HEIGHT / 2f, halfZ);
        attach(new Geometry("ObstacleBox", box), cx, HEIGHT / 2f, cz, yaw);
    }

    private void renderCylinder(float cx, float cz, float radius) {
        Cylinder cylinder = new Cylinder(16, 16, radius, HEIGHT);
        attach(new Geometry("ObstacleCylinder", cylinder), cx, HEIGHT / 2f, cz, 0f);
    }

    private void attach(Geometry geometry, float cx, float cy, float cz, float yaw) {
        geometry.setMaterial(createMaterial());
        geometry.setLocalTranslation(cx, cy, cz);
        if (yaw != 0f) {
            geometry.setLocalRotation(new Quaternion().fromAngleAxis(yaw, Vector3f.UNIT_Y));
        }
        sceneRoot.attachChild(geometry);
    }

    private Material createMaterial() {
        Material material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        material.setBoolean("UseMaterialColors", true);
        material.setColor("Diffuse", COLOR);
        material.setColor("Ambient", AMBIENT);
        material.setColor("Specular", ColorRGBA.White);
        material.setFloat("Shininess", 16f);
        return material;
    }
}
