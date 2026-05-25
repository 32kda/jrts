package com.jrts.tools.importer;

import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CollisionBakerTest {

    private CollisionBaker baker;

    @BeforeEach
    void setUp() {
        baker = new CollisionBaker();
    }

    @Test
    void bakeAABBFromSingleBoxMesh() {
        Node chassis = new Node("Chassis");
        Geometry boxGeom = createBoxGeometry(1f, 1f, 1f);
        chassis.attachChild(boxGeom);

        ModelManifest.CollisionShapeData aabb = baker.bakeAABB(chassis);
        assertEquals("rect", aabb.type());
        assertEquals(0f, aabb.center()[0], 0.001f);
        assertEquals(0f, aabb.center()[1], 0.001f);
        assertEquals(0f, aabb.center()[2], 0.001f);
        assertEquals(0.5f, aabb.halfExtents()[0], 0.001f);
        assertEquals(0.5f, aabb.halfExtents()[1], 0.001f);
        assertEquals(0.5f, aabb.halfExtents()[2], 0.001f);
    }

    @Test
    void bakeRadiusFromAABB() {
        Node chassis = new Node("Chassis");
        chassis.attachChild(createBoxGeometry(2f, 3f, 1f));

        ModelManifest.CollisionShapeData aabb = baker.bakeAABB(chassis);
        ModelManifest.CollisionShapeData radial = baker.bakeRadius(aabb);

        assertEquals("radial", radial.type());
        assertEquals(1.5f, radial.radius(), 0.001f);
    }

    @Test
    void nullChassisReturnsZeroSize() {
        ModelManifest.CollisionShapeData aabb = baker.bakeAABB(null);
        assertEquals("rect", aabb.type());
        assertEquals(0f, aabb.halfExtents()[0], 0.001f);
    }

    @Test
    void emptyChassisReturnsZeroSize() {
        Node chassis = new Node("Chassis");
        ModelManifest.CollisionShapeData aabb = baker.bakeAABB(chassis);
        assertEquals(0f, aabb.halfExtents()[0], 0.001f);
    }

    @Test
    void autoShapeSoldierReturnsRadial() {
        Node chassis = new Node("Chassis");
        chassis.attachChild(createBoxGeometry(1f, 2f, 1f));

        ModelManifest.CollisionShapeData shape = baker.autoShape("Soldier", chassis);
        assertEquals("radial", shape.type());
    }

    @Test
    void autoShapeVehicleReturnsRect() {
        Node chassis = new Node("Chassis");
        chassis.attachChild(createBoxGeometry(1f, 2f, 1f));

        ModelManifest.CollisionShapeData shape = baker.autoShape("AFV", chassis);
        assertEquals("rect", shape.type());
    }

    @Test
    void aabbCenterIsCorrectForOffsetMesh() {
        Node chassis = new Node("Chassis");
        Geometry geom = createBoxGeometry(2f, 2f, 2f);
        geom.setLocalTranslation(5, 0, 0);
        chassis.attachChild(geom);

        ModelManifest.CollisionShapeData aabb = baker.bakeAABB(chassis);
        assertEquals(5f, aabb.center()[0], 0.01f);
        assertEquals(1f, aabb.halfExtents()[0], 0.01f);
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

        mesh.setBuffer(VertexBuffer.Type.Position, 3,
                BufferUtils.createFloatBuffer(vertices));
        mesh.setBuffer(VertexBuffer.Type.Index, 1,
                BufferUtils.createIntBuffer(indices));
        mesh.updateBound();

        return new Geometry("TestBox", mesh);
    }
}
