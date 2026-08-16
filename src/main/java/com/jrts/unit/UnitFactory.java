package com.jrts.unit;

import com.jrts.config.UnitConfig;
import com.jrts.movement.MovementControl;
import com.jrts.rendering.LoadedModel;
import com.jrts.scene.TerrainHeightProvider;
import com.jrts.tools.importer.ModelManifest;
import com.jrts.turret.TurretConfig;
import com.jrts.turret.TurretControl;
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
 * - Attach per-unit behaviours (turret, movement) as JME Controls
 * - Register the unit in UnitRegistry
 */
public class UnitFactory {

    private static final Logger log = LoggerFactory.getLogger(UnitFactory.class);

    private final UnitRegistry registry;
    private final TerrainHeightProvider terrain;
    private final AtomicInteger nextId = new AtomicInteger(1);

    public UnitFactory(UnitRegistry registry, TerrainHeightProvider terrain) {
        this.registry = registry;
        this.terrain = terrain;
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
        attachTurretControl(unit, config, spatial);
        attachMovementControl(unit, spatial);

        registry.register(unit);
        log.info("Created unit: id={}, name={}, pos=({},{},{}), yaw={}",
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
                case BARREL_PIVOT -> {
                    Node barrelPivot = findNodeByPath(spatial, nodePath);
                    if (barrelPivot != null) {
                        unit.setBarrelPivot(barrelPivot);
                        log.debug("Resolved BarrelPivot for unit {}: {}", unit.getId(), nodePath);
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
     * Attaches a {@link TurretControl} to turret units so JME's scene-graph
     * update traversal drives the turret state machine each frame.
     */
    private void attachTurretControl(Unit unit, UnitConfig config, Node spatial) {
        if (!unit.hasTurret() || unit.getTurretPivot() == null) {
            return;
        }
        TurretConfig turretConfig = TurretConfig.from(config.turrets());
        TurretControl control = new TurretControl(
                turretConfig, unit.getTurretPivot(), unit.getBarrelPivot());
        spatial.addControl(control);
        log.debug("Attached TurretControl to unit {} (allowsPitch={})",
                unit.getId(), turretConfig.allowsPitch());
    }

    /**
     * Attaches a {@link MovementControl} to movable units so JME's scene-graph
     * update traversal advances them along their waypoints each frame.
     */
    private void attachMovementControl(Unit unit, Node spatial) {
        if (!unit.canMove()) {
            return;
        }
        spatial.addControl(new MovementControl(unit, terrain));
        log.debug("Attached MovementControl to unit {}", unit.getId());
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

        int start = 0;
        if (parts.length > 0 && root.getName() != null
                && parts[0].equalsIgnoreCase(root.getName())) {
            start = 1;
        }

        for (int i = start; i < parts.length; i++) {
            String part = parts[i];
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
