package com.jrts.tools.importer;

import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Stage 2 of the import pipeline.
 * Walks the JME Node tree and classifies nodes by their name
 * against the Blender naming convention (case-insensitive regex match).
 *
 * Detected node roles are stored in a ModelManifest.
 * A single node can match at most one role.
 *
 * The classification is order-dependent: more specific patterns
 * checked before general ones (e.g. TurretPivot before Turret).
 */
public class EmptyNodeResolver {

    private static final Logger log = LoggerFactory.getLogger(EmptyNodeResolver.class);

    private static final Map<Pattern, NodeRole> ROLE_PATTERNS = new LinkedHashMap<>();

    static {
        ROLE_PATTERNS.put(Pattern.compile("(?i).*turretpivot.*"), NodeRole.TURRET_PIVOT);
        ROLE_PATTERNS.put(Pattern.compile("(?i).*barrelpivot.*"), NodeRole.BARREL_PIVOT);
        ROLE_PATTERNS.put(Pattern.compile("(?i).*muzzle(?!.*flash)(?!.*smoke).*"), NodeRole.MUZZLE);
        ROLE_PATTERNS.put(Pattern.compile("(?i).*dockingpoint.*"), NodeRole.DOCKING_POINT);
        ROLE_PATTERNS.put(Pattern.compile("(?i).*track(s)?.*"), NodeRole.TRACKS);
        ROLE_PATTERNS.put(Pattern.compile("(?i).*smoke.*"), NodeRole.SMOKE);
        ROLE_PATTERNS.put(Pattern.compile("(?i).*spawnpoint.*"), NodeRole.SPAWN_POINT);
        ROLE_PATTERNS.put(Pattern.compile("(?i).*exitpoint.*"), NodeRole.EXIT_POINT);
        ROLE_PATTERNS.put(Pattern.compile("(?i).*dockpoint.*"), NodeRole.DOCK_POINT);
        ROLE_PATTERNS.put(Pattern.compile("(?i).*wheel_fl.*"), NodeRole.WHEEL_FL);
        ROLE_PATTERNS.put(Pattern.compile("(?i).*wheel_fr.*"), NodeRole.WHEEL_FR);
        ROLE_PATTERNS.put(Pattern.compile("(?i).*wheel_rl.*"), NodeRole.WHEEL_RL);
        ROLE_PATTERNS.put(Pattern.compile("(?i).*wheel_rr.*"), NodeRole.WHEEL_RR);
        ROLE_PATTERNS.put(Pattern.compile("(?i).*chassis.*"), NodeRole.CHASSIS);
        ROLE_PATTERNS.put(Pattern.compile("(?i).*turret(?!.*pivot).*"), NodeRole.TURRET_MESH);
        ROLE_PATTERNS.put(Pattern.compile("(?i).*barrel(?!.*pivot).*"), NodeRole.BARREL_MESH);
        ROLE_PATTERNS.put(Pattern.compile("(?i).*ramp.*"), NodeRole.RAMP);
    }

    /**
     * @param rootNode the root of the imported glTF tree
     * @return populated ModelManifest with all matched roles
     */
    public ModelManifest resolve(Node rootNode) {
        log.info("Resolving empty nodes from model hierarchy");
        ModelManifest manifest = new ModelManifest();
        resolveRecursive(rootNode, "", manifest);
        log.info("Resolved {} node roles", manifest.getRoles().size());
        return manifest;
    }

    private void resolveRecursive(Node node, String parentPath, ModelManifest manifest) {
        String name = node.getName() != null ? node.getName() : "";
        String currentPath = parentPath.isEmpty() ? name : parentPath + "/" + name;

        for (Map.Entry<Pattern, NodeRole> entry : ROLE_PATTERNS.entrySet()) {
            if (entry.getKey().matcher(name).matches()) {
                manifest.registerRole(entry.getValue(), currentPath);
                log.debug("Matched node '{}' → role={}", name, entry.getValue());
                break;
            }
        }

        for (Spatial child : node.getChildren()) {
            if (child instanceof Node childNode) {
                resolveRecursive(childNode, currentPath, manifest);
            }
        }
    }
}
