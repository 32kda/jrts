package com.jrts.selectable;

import com.jrts.unit.Unit;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jme3.math.FastMath;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Renders a circular highlight decal under each selected unit.
 *
 * For Stage 1, uses a JME Quad with a colored material placed at the unit's
 * ground position (Y slightly above terrain to avoid z-fighting).
 *
 * Observes SelectionSystem and toggles visibility per unit.
 */
public class SelectionHighlight implements com.jrts.input.SelectionSystem.SelectionObserver {

    private static final Logger log = LoggerFactory.getLogger(SelectionHighlight.class);

    private static final float DECAL_SIZE = 3f;
    private static final float Y_OFFSET = 0.05f;

    private final Node sceneRoot;
    private final Material highlightMaterial;
    private final Map<Unit, Geometry> highlights = new HashMap<>();

    public SelectionHighlight(Node sceneRoot, com.jme3.asset.AssetManager assetManager) {
        this.sceneRoot = sceneRoot;

        this.highlightMaterial = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");
        this.highlightMaterial.setColor("Color", new ColorRGBA(0.2f, 1.0f, 0.2f, 0.5f));
        this.highlightMaterial.getAdditionalRenderState().setDepthWrite(false);

        log.info("SelectionHighlight initialized");
    }

    @Override
    public void onSelectionChanged(Set<Unit> selected) {
        for (var entry : new HashMap<>(highlights).entrySet()) {
            if (!selected.contains(entry.getKey())) {
                entry.getValue().removeFromParent();
                highlights.remove(entry.getKey());
            }
        }

        for (Unit unit : selected) {
            if (!highlights.containsKey(unit)) {
                Geometry decal = createDecal();
                highlights.put(unit, decal);
                unit.getSpatial().attachChild(decal);
            }
        }

        updateDecalPositions();
        log.debug("Selection highlight updated: {} highlights", highlights.size());
    }

    private Geometry createDecal() {
        Quad quad = new Quad(DECAL_SIZE, DECAL_SIZE);
        Geometry geom = new Geometry("selectionDecal", quad);
        geom.setMaterial(highlightMaterial);
        geom.rotate(-FastMath.HALF_PI, 0, 0);
        geom.setLocalTranslation(-DECAL_SIZE / 2f, Y_OFFSET, -DECAL_SIZE / 2f);
        return geom;
    }

    private void updateDecalPositions() {
        for (var entry : highlights.entrySet()) {
            Vector3f pos = entry.getKey().getPosition();
            entry.getValue().setLocalTranslation(-DECAL_SIZE / 2f,
                    Y_OFFSET, DECAL_SIZE / 2f);
        }
    }

}
