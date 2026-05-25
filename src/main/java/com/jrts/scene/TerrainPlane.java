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

/**
 * Visual terrain representation for Stage 1.
 * A single flat quad with tan/sand material.
 * Height data is provided by TerrainHeightProvider (constant 0 in Stage 1).
 *
 * Does NOT implement collision — all collision goes through
 * TerrainHeightProvider.getHeight() instead.
 */
public class TerrainPlane {

    private static final Logger log = LoggerFactory.getLogger(TerrainPlane.class);

    private final Geometry geometry;
    private final float width;
    private final float depth;

    public TerrainPlane(float width, float depth, float height,
                        AssetManager assetManager) {
        this.width = width;
        this.depth = depth;

        Mesh mesh = createFlatQuad(width, depth, height);
        this.geometry = new Geometry("Terrain", mesh);

        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Diffuse", new ColorRGBA(0.76f, 0.60f, 0.42f, 1.0f));
        mat.setColor("Ambient", new ColorRGBA(0.3f, 0.25f, 0.15f, 1.0f));
        mat.setFloat("Shininess", 1f);
        this.geometry.setMaterial(mat);

        log.info("TerrainPlane created: {}x{} at height={}", width, depth, height);
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public float getWidth() {
        return width;
    }

    public float getDepth() {
        return depth;
    }

    private static Mesh createFlatQuad(float width, float depth, float height) {
        Mesh mesh = new Mesh();
        float hw = width / 2f;
        float hd = depth / 2f;

        float[] vertices = {
                -hw, height, -hd,
                hw, height, -hd,
                hw, height, hd,
                -hw, height, hd
        };

        float[] normals = {
                0, 1, 0,
                0, 1, 0,
                0, 1, 0,
                0, 1, 0
        };

        float[] uvs = {
                0, 0,
                width, 0,
                width, depth,
                0, depth
        };

        int[] indices = {
                0, 1, 2,
                0, 2, 3
        };

        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
        mesh.setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(normals));
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(uvs));
        mesh.setBuffer(VertexBuffer.Type.Index, 1, BufferUtils.createIntBuffer(indices));
        mesh.updateBound();

        return mesh;
    }
}
