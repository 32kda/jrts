package com.jrts.scene;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A flat terrain grid for Stage 1 visualisation.
 * Renders a solid ground plane with a line grid overlay
 * so camera zoom, pitch, and yaw changes are visually obvious.
 *
 * No collision — all height queries go through TerrainHeightProvider.
 */
public class TerrainGrid {

    private static final Logger log = LoggerFactory.getLogger(TerrainGrid.class);

    private final Geometry groundGeom;
    private final Geometry gridGeom;
    private final float width;
    private final float depth;

    /**
     * @param width   world units (e.g. 1000 for 1km)
     * @param depth   world units
     * @param spacing grid line spacing in world units (e.g. 50)
     * @param height  terrain Y
     * @param am      JME AssetManager
     */
    public TerrainGrid(float width, float depth, float spacing, float height,
                       AssetManager am) {
        this.width = width;
        this.depth = depth;

        this.groundGeom = createGroundGeom(width, depth, height, am);
        this.gridGeom = createGridGeom(width, depth, spacing, height, am);

        log.info("TerrainGrid created: {}x{} with {}m spacing at height={}",
                width, depth, spacing, height);
    }

    public Geometry getGroundGeometry() {
        return groundGeom;
    }

    public Geometry getGridGeometry() {
        return gridGeom;
    }

    public float getWidth() {
        return width;
    }

    public float getDepth() {
        return depth;
    }

    private static Geometry createGroundGeom(float w, float d, float h,
                                             AssetManager am) {
        Mesh mesh = createFlatQuad(w, d, h);
        Geometry geom = new Geometry("Ground", mesh);

        Material mat = new Material(am, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Diffuse", new ColorRGBA(0.25f, 0.45f, 0.18f, 1.0f));
        mat.setColor("Ambient", new ColorRGBA(0.15f, 0.25f, 0.10f, 1.0f));
        mat.setFloat("Shininess", 4f);
        geom.setMaterial(mat);

        return geom;
    }

    private static Geometry createGridGeom(float w, float d, float spacing,
                                           float h, AssetManager am) {
        List<Float> vertices = new ArrayList<>();
        float hw = w / 2f;
        float hd = d / 2f;
        float y = h + 0.02f;

        for (float x = -hw; x <= hw + 0.001f; x += spacing) {
            vertices.add(x); vertices.add(y); vertices.add(-hd);
            vertices.add(x); vertices.add(y); vertices.add(hd);
        }
        for (float z = -hd; z <= hd + 0.001f; z += spacing) {
            vertices.add(-hw); vertices.add(y); vertices.add(z);
            vertices.add(hw); vertices.add(y); vertices.add(z);
        }

        float[] verts = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            verts[i] = vertices.get(i);
        }

        Mesh mesh = new Mesh();
        mesh.setMode(Mesh.Mode.Lines);
        mesh.setBuffer(VertexBuffer.Type.Position, 3,
                BufferUtils.createFloatBuffer(verts));
        mesh.updateBound();

        Geometry geom = new Geometry("Grid", mesh);
        Material mat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(0f, 0f, 0f, 0.25f));
        mat.getAdditionalRenderState().setLineWidth(1f);
        geom.setMaterial(mat);

        return geom;
    }

    private static Mesh createFlatQuad(float w, float d, float h) {
        Mesh mesh = new Mesh();
        float hw = w / 2f;
        float hd = d / 2f;

        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(
                new float[]{-hw, h, -hd, hw, h, -hd, hw, h, hd, -hw, h, hd}));
        mesh.setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(
                new float[]{0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0}));
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(
                new float[]{0, 0, w, 0, w, d, 0, d}));
        mesh.setBuffer(VertexBuffer.Type.Index, 1, BufferUtils.createIntBuffer(
                new int[]{0, 1, 2, 0, 2, 3}));
        mesh.updateBound();
        return mesh;
    }
}
