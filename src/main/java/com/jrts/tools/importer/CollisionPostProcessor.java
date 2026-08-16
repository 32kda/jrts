package com.jrts.tools.importer;

import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.moandjiezana.toml.Toml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * Stage 3 of the import pipeline (post-processing).
 * Reads collision config from a TOML file alongside the .glb model
 * and bakes collision shape + visibility sphere into the manifest.
 *
 * Priority:
 * 1. TOML [collision] type="cylinder" / "sphere" / "box" with explicit dims
 * 2. TOML [collision] type="auto" or missing → auto-detect
 * 3. Auto: find chassis/body/hull node → bake box AABB; otherwise → full-model sphere
 *
 * Always computes a visibility sphere (max vertex distance from origin).
 */
public class CollisionPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(CollisionPostProcessor.class);

    private static final Pattern CHASSIS_NAMES = Pattern.compile(
            "(?i).*(chassis|body|hull).*");

    private final CollisionBaker baker = new CollisionBaker();

    /**
     * @param rootNode  loaded JME node tree
     * @param manifest  manifest with resolved node roles
     * @param glbPath   path to the source .glb file (used to find sibling .toml)
     */
    public void process(Node rootNode, ModelManifest manifest, Path glbPath) {
        rootNode.updateGeometricState();

        float visRadius = computeVisibilitySphereRadius(rootNode);
        manifest.setVisibilitySphereRadius(visRadius);
        log.info("Visibility sphere radius: {}", visRadius);

        String modelName = glbPath.getFileName().toString().replace(".glb", "");
        Path tomlPath = glbPath.getParent().resolve(modelName + ".toml");

        CollisionConfig config = readCollisionConfig(tomlPath);

        ModelManifest.CollisionShapeData shape;
        if (config == null || config.isAuto()) {
            log.info("Collision: auto-detect (no TOML or type=auto)");
            shape = autoDetectShape(rootNode, manifest);
        } else {
            log.info("Collision: TOML type={}", config.type);
            shape = bakeFromConfig(config);
        }

        manifest.setCollision(shape);
        log.info("Collision shape: type={}, center=({},{},{})",
                shape.type(), shape.center()[0], shape.center()[1], shape.center()[2]);
    }

    private CollisionConfig readCollisionConfig(Path tomlPath) {
        if (!Files.exists(tomlPath)) {
            log.info("No TOML config found: {}", tomlPath);
            return null;
        }

        try (BufferedReader reader = Files.newBufferedReader(tomlPath)) {
            Toml toml = new Toml().read(reader);
            String type = toml.getString("collision.type");
            if (type == null) {
                log.info("No [collision] section in TOML: {}", tomlPath);
                return null;
            }

            CollisionConfig config = new CollisionConfig();
            config.type = type.trim().toLowerCase();

            if ("cylinder".equals(config.type)) {
                config.radius = safeGetFloat(toml, "collision.radius", 1.0f);
                config.height = safeGetFloat(toml, "collision.height", 1.0f);
            } else if ("sphere".equals(config.type)) {
                config.radius = safeGetFloat(toml, "collision.radius", 1.0f);
            } else if ("box".equals(config.type)) {
                config.length = safeGetFloat(toml, "collision.length", 1.0f);
                config.width = safeGetFloat(toml, "collision.width", 1.0f);
                config.height = safeGetFloat(toml, "collision.height", 1.0f);
            } else if (!"auto".equals(config.type) && !"capsule".equals(config.type)) {
                log.warn("Unknown collision type '{}', falling back to auto", config.type);
                return null;
            }

            return config;
        } catch (Exception e) {
            log.warn("Failed to read collision TOML: {}", e.getMessage());
            return null;
        }
    }

    private ModelManifest.CollisionShapeData autoDetectShape(Node rootNode, ModelManifest manifest) {
        Node chassisNode = findChassisNode(rootNode, manifest);
        if (chassisNode != null) {
            log.info("Auto-detected chassis node, baking AABB box");
            return baker.bakeAABB(chassisNode);
        }

        log.info("No chassis node found, baking from full model");
        return baker.bakeAABB(rootNode);
    }

    private ModelManifest.CollisionShapeData bakeFromConfig(CollisionConfig config) {
        return switch (config.type) {
            case "cylinder" -> new ModelManifest.CollisionShapeData(
                    "cylinder",
                    new float[]{0, config.height / 2f, 0},
                    new float[]{config.radius, config.radius, config.height / 2f},
                    config.radius,
                    config.height);
            case "sphere" -> new ModelManifest.CollisionShapeData(
                    "sphere",
                    new float[]{0, 0, 0},
                    new float[]{0, 0, 0},
                    config.radius,
                    0f);
            case "box" -> new ModelManifest.CollisionShapeData(
                    "box",
                    new float[]{0, 0, 0},
                    new float[]{config.length / 2f, config.width / 2f, config.height / 2f},
                    0f,
                    0f);
            default -> new ModelManifest.CollisionShapeData(
                    "rect",
                    new float[]{0, 0, 0},
                    new float[]{0, 0, 0},
                    0f,
                    0f);
        };
    }

    private Node findChassisNode(Node rootNode, ModelManifest manifest) {
        if (manifest.hasRole(NodeRole.CHASSIS)) {
            String chassisPath = manifest.getNodePath(NodeRole.CHASSIS);
            Node found = findNodeByPath(rootNode, chassisPath);
            if (found != null) {
                log.info("Found chassis via manifest role: {}", chassisPath);
                return found;
            }
        }

        AtomicReference<Node> result = new AtomicReference<>();
        findNodeByNamePattern(rootNode, CHASSIS_NAMES, result);
        if (result.get() != null) {
            log.info("Found chassis via name pattern: {}", result.get().getName());
            return result.get();
        }

        return null;
    }

    static Node findNodeByPath(Node root, String path) {
        if (path == null || path.isEmpty()) {
            return root;
        }
        String[] parts = path.split("/");
        Node current = root;
        int start = 0;
        if (root.getName() != null && parts[0].equals(root.getName())) {
            start = 1;
        }
        for (int i = start; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) {
                continue;
            }
            Node found = findChildByName(current, part);
            if (found != null) {
                current = found;
            } else {
                return null;
            }
        }
        return current;
    }

    private static Node findChildByName(Node parent, String name) {
        for (Spatial child : parent.getChildren()) {
            if (child instanceof Node childNode && name.equals(childNode.getName())) {
                return childNode;
            }
        }
        return null;
    }

    private static void findNodeByNamePattern(Node node, Pattern pattern, AtomicReference<Node> result) {
        if (result.get() != null) {
            return;
        }
        String name = node.getName();
        if (name != null && pattern.matcher(name).matches()) {
            result.set(node);
            return;
        }
        for (Spatial child : node.getChildren()) {
            if (child instanceof Node childNode) {
                findNodeByNamePattern(childNode, pattern, result);
            }
        }
    }

    static float computeVisibilitySphereRadius(Node rootNode) {
        float[] maxDistSq = {0f};
        collectMaxDistSq(rootNode, maxDistSq);
        return (float) Math.sqrt(maxDistSq[0]);
    }

    private static void collectMaxDistSq(Node node, float[] maxDistSq) {
        for (Spatial child : node.getChildren()) {
            if (child instanceof Geometry geom) {
                var mesh = geom.getMesh();
                if (mesh != null) {
                    var posBuf = mesh.getBuffer(com.jme3.scene.VertexBuffer.Type.Position);
                    if (posBuf != null) {
                        var data = (java.nio.FloatBuffer) posBuf.getData();
                        data.rewind();
                        while (data.hasRemaining()) {
                            float x = data.get();
                            float y = data.get();
                            float z = data.get();
                            var world = geom.getWorldMatrix().mult(new com.jme3.math.Vector3f(x, y, z));
                            float d2 = world.x * world.x + world.y * world.y + world.z * world.z;
                            if (d2 > maxDistSq[0]) {
                                maxDistSq[0] = d2;
                            }
                        }
                    }
                }
            }
            if (child instanceof Node childNode) {
                collectMaxDistSq(childNode, maxDistSq);
            }
        }
    }

    private static float safeGetFloat(Toml toml, String key, float defaultValue) {
        try {
            Double d = toml.getDouble(key);
            if (d != null) return d.floatValue();
        } catch (Exception ignored) {
        }
        try {
            Long l = toml.getLong(key);
            if (l != null) return l.floatValue();
        } catch (Exception ignored) {
        }
        return defaultValue;
    }

    private static class CollisionConfig {
        String type;
        float radius;
        float height;
        float length;
        float width;

        boolean isAuto() {
            return "auto".equals(type);
        }
    }
}
