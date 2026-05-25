package com.jrts.tools.importer;

import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;

/**
 * Stage 3 of the import pipeline.
 * Computes collision bounds from the Chassis mesh vertex data.
 *
 * Two shape types supported:
 * - RectShape (AABB): full axis-aligned bounding box
 * - RadialShape (Sphere): minimal bounding sphere derived from AABB
 */
public class CollisionBaker {

    private static final Logger log = LoggerFactory.getLogger(CollisionBaker.class);

    /**
     * Traverses all Geometry children of chassisNode, collects vertex
     * positions transformed by each geometry's world matrix, and computes
     * the minimal AABB.
     *
     * @param chassisNode root of the chassis sub-tree
     * @return ModelManifest.CollisionShapeData with rect type
     */
    public ModelManifest.CollisionShapeData bakeAABB(Node chassisNode) {
        if (chassisNode == null) {
            log.warn("Chassis node is null, returning zero-size AABB");
            return new ModelManifest.CollisionShapeData("rect",
                    new float[]{0, 0, 0},
                    new float[]{0, 0, 0},
                    0f);
        }

        chassisNode.updateGeometricState();

        Vector3f min = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
        Vector3f max = new Vector3f(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);
        boolean[] hasVertices = {false};

        collectVertexBounds(chassisNode, min, max, hasVertices);

        if (!hasVertices[0]) {
            log.warn("Chassis has no geometry, returning zero-size AABB");
            return new ModelManifest.CollisionShapeData("rect",
                    new float[]{0, 0, 0},
                    new float[]{0, 0, 0},
                    0f);
        }

        Vector3f center = min.add(max).multLocal(0.5f);
        Vector3f halfExtents = max.subtract(min).multLocal(0.5f);

        log.info("Baked AABB: center=({:.2f},{:.2f},{:.2f}), halfExtents=({:.2f},{:.2f},{:.2f})",
                center.x, center.y, center.z,
                halfExtents.x, halfExtents.y, halfExtents.z);

        return new ModelManifest.CollisionShapeData("rect",
                new float[]{center.x, center.y, center.z},
                new float[]{halfExtents.x, halfExtents.y, halfExtents.z},
                0f);
    }

    /**
     * Derives bounding-sphere radius from an AABB:
     * radius = max(halfExtents.x, halfExtents.y, halfExtents.z)
     */
    public ModelManifest.CollisionShapeData bakeRadius(ModelManifest.CollisionShapeData aabb) {
        float[] he = aabb.halfExtents();
        float radius = Math.max(Math.max(he[0], he[1]), he[2]);

        log.info("Baked radius: {:.2f} from AABB halfExtents=({:.2f},{:.2f},{:.2f})",
                radius, he[0], he[1], he[2]);

        return new ModelManifest.CollisionShapeData("radial",
                aabb.center(),
                new float[]{0, 0, 0},
                radius);
    }

    /**
     * Determine shape type from category and bake accordingly.
     */
    public ModelManifest.CollisionShapeData autoShape(String category, Node chassisNode) {
        ModelManifest.CollisionShapeData aabb = bakeAABB(chassisNode);
        if ("Soldier".equalsIgnoreCase(category)) {
            return bakeRadius(aabb);
        }
        return aabb;
    }

    private void collectVertexBounds(Node node, Vector3f min, Vector3f max, boolean[] hasVertices) {
        for (Spatial child : node.getChildren()) {
            if (child instanceof Geometry geom) {
                Mesh mesh = geom.getMesh();
                if (mesh != null) {
                    VertexBuffer posBuf = mesh.getBuffer(VertexBuffer.Type.Position);
                    if (posBuf != null) {
                        FloatBuffer data = (FloatBuffer) posBuf.getData();
                        data.rewind();
                        while (data.hasRemaining()) {
                            float x = data.get();
                            float y = data.get();
                            float z = data.get();
                            Vector3f local = geom.getWorldMatrix().mult(new Vector3f(x, y, z));
                            min.minLocal(local);
                            max.maxLocal(local);
                            hasVertices[0] = true;
                        }
                    }
                }
            }
            if (child instanceof Node childNode) {
                collectVertexBounds(childNode, min, max, hasVertices);
            }
        }
    }
}
