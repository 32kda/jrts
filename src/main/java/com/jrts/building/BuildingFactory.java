package com.jrts.building;

import com.jrts.config.BuildingConfig;
import com.jrts.rendering.LoadedModel;
import com.jrts.tools.importer.ModelManifest;
import com.jrts.tools.importer.NodeRole;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Creates a {@link Building} from a {@link BuildingConfig} and a loaded model.
 * Assigns a unique runtime ID, sets position/yaw, resolves named empty nodes
 * (turret/spawn/exit/dock/smoke/ramp) from the model manifest, and registers the result.
 */
public class BuildingFactory {

    private static final Logger log = LoggerFactory.getLogger(BuildingFactory.class);

    private final BuildingRegistry registry;
    private final AtomicInteger nextId = new AtomicInteger(1);

    public BuildingFactory(BuildingRegistry registry) {
        this.registry = registry;
    }

    /**
     * @param config   parsed building config
     * @param model    loaded model (spatial + manifest)
     * @param position initial world position
     * @param yaw      initial facing (radians)
     * @return a new, registered building
     */
    public Building create(BuildingConfig config, LoadedModel model, Vector3f position, float yaw) {
        int id = nextId.getAndIncrement();
        Node spatial = model.spatial();
        spatial.setLocalTranslation(position);
        spatial.setLocalRotation(new Quaternion().fromAngleAxis(yaw, Vector3f.UNIT_Y));

        Building building = new Building(id, config, spatial);
        building.setBodyYaw(yaw);

        resolveEmptyNodes(building, spatial, model.manifest());

        registry.register(building);
        log.info("Created building: id={}, name={}, pos=({},{},{}), turret={}, spawn={}, exit={}, dock={}",
                id, config.identity().name(), position.x, position.y, position.z,
                building.getTurretPivot() != null, building.getSpawnPoint() != null,
                building.getExitPoint() != null, building.getDockPoint() != null);
        return building;
    }

    /**
     * Resolve TurretPivot, SpawnPoint, ExitPoint, DockPoint, Smoke, Ramp and Muzzle
     * empty nodes from the manifest and link them to the Building for runtime access.
     */
    private void resolveEmptyNodes(Building building, Node spatial, ModelManifest manifest) {
        if (manifest == null) {
            return;
        }
        for (var entry : manifest.getRoles().entrySet()) {
            Node node = findNodeByPath(spatial, entry.getValue());
            if (node == null) {
                log.warn("Node '{}' for role {} not found in building {}", entry.getValue(),
                        entry.getKey(), building.getId());
                continue;
            }
            setRole(building, entry.getKey(), node);
        }
    }

    private void setRole(Building building, NodeRole role, Node node) {
        switch (role) {
            case TURRET_PIVOT -> building.setTurretPivot(node);
            case BARREL_PIVOT -> building.setBarrelPivot(node);
            case MUZZLE -> building.setMuzzle(node);
            case SPAWN_POINT -> building.setSpawnPoint(node);
            case EXIT_POINT -> building.setExitPoint(node);
            case DOCK_POINT -> building.setDockPoint(node);
            case SMOKE -> building.setSmokePoint(node);
            case RAMP -> building.setRampPoint(node);
            default -> {
                // other roles (base mesh, wheels, etc.) are not needed at runtime.
            }
        }
    }

    /**
     * Find a Node in the spatial hierarchy by its name-based path.
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
                return null;
            }
        }
        return current;
    }
}
