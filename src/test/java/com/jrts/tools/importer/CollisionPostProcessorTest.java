package com.jrts.tools.importer;

import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers CollisionPostProcessor: TOML-driven shapes (cylinder/sphere/box),
 * auto-detection (chassis box vs full-model AABB), visibility sphere, and
 * node-path resolution.
 */
class CollisionPostProcessorTest {

    @TempDir
    Path tempDir;

    private final CollisionPostProcessor processor = new CollisionPostProcessor();

    @Test
    void cylinderFromToml() throws Exception {
        writeToml("[collision]\ntype = \"cylinder\"\nradius = 1.5\nheight = 1.2\n");

        ModelManifest manifest = new ModelManifest();
        processor.process(sceneWithBox(2, 2, 2), manifest, glbPath());

        assertEquals("cylinder", manifest.getCollision().type());
        assertEquals(1.5f, manifest.getCollision().radius(), 0.001f);
        assertEquals(1.2f, manifest.getCollision().height(), 0.001f);
        assertEquals(0.6f, manifest.getCollision().center()[1], 0.001f);
    }

    @Test
    void sphereFromToml() throws Exception {
        writeToml("[collision]\ntype = \"sphere\"\nradius = 2.5\n");

        ModelManifest manifest = new ModelManifest();
        processor.process(sceneWithBox(2, 2, 2), manifest, glbPath());

        assertEquals("sphere", manifest.getCollision().type());
        assertEquals(2.5f, manifest.getCollision().radius(), 0.001f);
    }

    @Test
    void boxFromToml() throws Exception {
        writeToml("[collision]\ntype = \"box\"\nlength = 2.0\nwidth = 3.0\nheight = 4.0\n");

        ModelManifest manifest = new ModelManifest();
        processor.process(sceneWithBox(2, 2, 2), manifest, glbPath());

        assertEquals("box", manifest.getCollision().type());
        assertArrayEquals(new float[]{1.0f, 1.5f, 2.0f}, manifest.getCollision().halfExtents(), 0.001f);
    }

    @Test
    void autoTypeDetectsChassisBox() throws Exception {
        writeToml("[collision]\ntype = \"auto\"\n");

        Node root = new Node("Scene");
        Node chassis = new Node("chassis");
        chassis.attachChild(createBoxGeometry(2, 3, 4));
        root.attachChild(chassis);

        ModelManifest manifest = new ModelManifest();
        manifest.registerRole(NodeRole.CHASSIS, "Scene/chassis");

        processor.process(root, manifest, glbPath());

        assertEquals("rect", manifest.getCollision().type());
        assertArrayEquals(new float[]{1.0f, 1.5f, 2.0f}, manifest.getCollision().halfExtents(), 0.001f);
    }

    @Test
    void missingTomlFallsBackToFullModelAabb() {
        Node root = new Node("Scene");
        root.attachChild(createBoxGeometry(2, 3, 4));

        ModelManifest manifest = new ModelManifest();
        processor.process(root, manifest, glbPath());

        assertEquals("rect", manifest.getCollision().type());
        assertArrayEquals(new float[]{1.0f, 1.5f, 2.0f}, manifest.getCollision().halfExtents(), 0.001f);
    }

    @Test
    void unknownTomlTypeFallsBackToAuto() throws Exception {
        writeToml("[collision]\ntype = \"foobar\"\n");

        Node root = new Node("Scene");
        root.attachChild(createBoxGeometry(2, 2, 2));

        ModelManifest manifest = new ModelManifest();
        processor.process(root, manifest, glbPath());

        assertEquals("rect", manifest.getCollision().type());
    }

    @Test
    void visibilitySphereRadiusMatchesMaxVertexDistance() {
        Node root = new Node("Scene");
        root.attachChild(createBoxGeometry(2, 2, 2));
        root.updateGeometricState();

        float radius = CollisionPostProcessor.computeVisibilitySphereRadius(root);
        assertEquals(Math.sqrt(3.0), radius, 0.001);
    }

    @Test
    void findNodeByPathSkipsRootName() {
        Node root = new Node("Scene");
        Node chassis = new Node("chassis");
        Node pivot = new Node("TurretPivot");
        chassis.attachChild(pivot);
        root.attachChild(chassis);

        assertSame(pivot, CollisionPostProcessor.findNodeByPath(root, "Scene/chassis/TurretPivot"));
        assertSame(pivot, CollisionPostProcessor.findNodeByPath(root, "chassis/TurretPivot"));
        assertNull(CollisionPostProcessor.findNodeByPath(root, "Scene/missing"));
    }

    private void writeToml(String content) throws Exception {
        Files.writeString(tempDir.resolve("tank.toml"), content);
    }

    private Path glbPath() {
        return tempDir.resolve("tank.glb");
    }

    private static Node sceneWithBox(float w, float h, float d) {
        Node root = new Node("Scene");
        root.attachChild(createBoxGeometry(w, h, d));
        return root;
    }

    private static Geometry createBoxGeometry(float w, float h, float d) {
        Mesh mesh = new Mesh();
        float hw = w / 2f;
        float hh = h / 2f;
        float hd = d / 2f;

        float[] vertices = {
                -hw, -hh, -hd, hw, -hh, -hd, hw, hh, -hd, -hw, hh, -hd,
                -hw, -hh, hd, hw, -hh, hd, hw, hh, hd, -hw, hh, hd
        };
        int[] indices = {
                0, 1, 2, 0, 2, 3, 4, 6, 5, 4, 7, 6,
                0, 4, 5, 0, 5, 1, 1, 5, 6, 1, 6, 2,
                2, 6, 7, 2, 7, 3, 4, 0, 3, 4, 3, 7
        };

        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
        mesh.setBuffer(VertexBuffer.Type.Index, 1, BufferUtils.createIntBuffer(indices));
        mesh.updateBound();

        return new Geometry("Box", mesh);
    }
}
