package com.jrts.tools.preview;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;

/**
 * Debug overlay toggling for AABB/radius collision shapes.
 * Renders wireframe box/circle around the selected model.
 */
public class CollisionWireframe {

    private final Node rootNode;
    private final AssetManager assetManager;
    private Geometry wireframeGeom;
    private boolean visible;

    public CollisionWireframe(Node rootNode, AssetManager assetManager) {
        this.rootNode = rootNode;
        this.assetManager = assetManager;
    }

    public void showBox(float[] center, float[] halfExtents) {
        hide();

        Box box = new Box(
                new com.jme3.math.Vector3f(center[0], center[1], center[2]),
                halfExtents[0], halfExtents[1], halfExtents[2]);

        wireframeGeom = new Geometry("CollisionWireframe", box);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(0, 1, 0, 0.3f));
        mat.getAdditionalRenderState().setWireframe(true);
        wireframeGeom.setMaterial(mat);

        rootNode.attachChild(wireframeGeom);
        visible = true;
    }

    public void hide() {
        if (wireframeGeom != null) {
            wireframeGeom.removeFromParent();
            wireframeGeom = null;
        }
        visible = false;
    }

    public void toggle() {
        if (visible) {
            hide();
        }
    }

    public boolean isVisible() {
        return visible;
    }
}
