package com.jrts.unit;

import com.jrts.config.UnitConfig;
import com.jrts.rendering.LoadedModel;
import com.jrts.tools.importer.ModelManifest;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Creates a Unit from a UnitConfig and a loaded 3D model.
 *
 * Responsibilities:
 * - Assign a unique runtime ID
 * - Compute UnitFlags from config
 * - Resolve empty-node references from ModelManifest
 * - Set initial position and rotation on the spatial
 * - Register the unit in UnitRegistry
 */
public class UnitFactory {

    private static final Logger log = LoggerFactory.getLogger(UnitFactory.class);

    private final UnitRegistry registry;
    private final AtomicInteger nextId = new AtomicInteger(1);

    public UnitFactory(UnitRegistry registry) {
        this.registry = registry;
    }

    /**
     * @param config   parsed TOML unit config
     * @param model    loaded .m3o (spatial + manifest)
     * @param position initial world position
     * @param yaw      initial body facing (radians)
     * @return a new, live Unit
     */
    public Unit create(UnitConfig config, LoadedModel model, Vector3f position, float yaw) {
        int id = nextId.getAndIncrement();
        int flags = UnitFlags.fromUnitConfig(config);

        Node spatial = model.spatial();
        spatial.setLocalTranslation(position);
        spatial.setLocalRotation(new Quaternion().fromAngleAxis(yaw, Vector3f.UNIT_Y));

        Unit unit = new Unit(id, config, spatial, flags);
        unit.setBodyYaw(yaw);

        resolveEmptyNodes(unit, spatial, model.manifest());

        registry.register(unit);
        log.info("Created unit: id={}, name={}, pos=({:.1f},{:.1f},{:.1f}), yaw={:.2f}",
                id, config.identity().displayName(), position.x, position.y, position.z, yaw);

        return unit;
    }

    /**
     * Resolve TurretPivot, Muzzle, and other empty nodes from manifest
     * and link them to the Unit for runtime access.
     */
    private void resolveEmptyNodes(Unit unit, Node spatial, ModelManifest manifest) {
        if (manifest == null) {
            return;
        }

        for (var entry : manifest.getRoles().entrySet()) {
            String nodePath = entry.getValue();
            switch (entry.getKey()) {
                case TURRET_PIVOT -> {
                    Node turretPivot = findNodeByPath(spatial, nodePath);
                    if (turretPivot != null) {
                        unit.setTurretPivot(turretPivot);
                        log.debug("Resolved TurretPivot for unit {}: {}", unit.getId(), nodePath);
                    }
                }
                case MUZZLE -> {
                    Node muzzle = findNodeByPath(spatial, nodePath);
                    if (muzzle != null) {
                        unit.setMuzzle(muzzle);
                        log.debug("Resolved Muzzle for unit {}: {}", unit.getId(), nodePath);
                    }
                }
                default -> {
                    // other roles not needed at runtime (Stage 1)
                }
            }
        }
    }

    /**
     * Find a Node in the spatial hierarchy by its path (name-based search).
     */
    private Node findNodeByPath(Node root, String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        String[] parts = path.split("/");
        Node current = root;

        for (String part : parts) {
            boolean found = false;
            for (Spatial child : current.getChildren()) {
                if (child instanceof Node childNode && part.equalsIgnoreCase(childNode.getName())) {
                    current = childNode;
                    found = true;
                    break;
                }
            }
            if (!found) {
                log.warn("Node not found in hierarchy: {}", path);
                return null;
            }
        }
        return current;
    }
}
