package com.jrts.input;

import com.jrts.scene.TerrainHeightProvider;
import com.jrts.unit.Unit;
import com.jrts.unit.UnitRegistry;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Raycasts from screen coordinates against the scene graph.
 *
 * Two modes:
 *   1. pickUnit(screenX, screenY) → Optional<Unit>
 *      Ray against all scene geometries, find closest with entityId UserData.
 *
 *   2. pickTerrain(screenX, screenY) → Optional<Vector3f>
 *      Ray against the terrain: uses terrainProvider.getHeight() for Y,
 *      NOT mesh collision.
 */
public class MousePicker {

    private static final Logger log = LoggerFactory.getLogger(MousePicker.class);

    private final Node sceneRoot;
    private final UnitRegistry unitRegistry;
    private final Camera cam;
    private final TerrainHeightProvider terrainProvider;

    public MousePicker(Node sceneRoot, UnitRegistry unitRegistry, Camera cam,
                       TerrainHeightProvider terrainProvider) {
        this.sceneRoot = sceneRoot;
        this.unitRegistry = unitRegistry;
        this.cam = cam;
        this.terrainProvider = terrainProvider;
        log.info("MousePicker initialized");
    }

    /**
     * Raycast for the closest selectable unit under the cursor.
     * Filters out spatials without entityId UserData.
     *
     * @return the Unit, or empty if no unit hit
     */
    public Optional<Unit> pickUnit(float screenX, float screenY) {
        Vector3f origin = cam.getWorldCoordinates(new Vector2f(screenX, screenY), 0f);
        Vector3f direction = cam.getWorldCoordinates(new Vector2f(screenX, screenY), 1f)
                .subtractLocal(origin).normalizeLocal();

        Ray ray = new Ray(origin, direction);
        CollisionResults results = new CollisionResults();
        sceneRoot.collideWith(ray, results);

        for (CollisionResult result : results) {
            Unit unit = findUnitFromGeometry(result.getGeometry());
            if (unit != null && unit.isSelectable()) {
                log.trace("Pick hit unit: {}", unit);
                return Optional.of(unit);
            }
        }

        return Optional.empty();
    }

    /**
     * Raycast cursor onto terrain plane.
     * Computes XZ intersection of camera ray with terrain plane,
     * then queries terrainProvider.getHeight() for the Y value.
     *
     * @return world position on terrain surface, or empty if off-map
     */
    public Optional<Vector3f> pickTerrain(float screenX, float screenY) {
        Vector3f origin = cam.getWorldCoordinates(new Vector2f(screenX, screenY), 0f);
        Vector3f direction = cam.getWorldCoordinates(new Vector2f(screenX, screenY), 1f)
                .subtractLocal(origin).normalizeLocal();

        if (direction.y >= 0) {
            return Optional.empty();
        }

        float t = -origin.y / direction.y;
        float wx = origin.x + t * direction.x;
        float wz = origin.z + t * direction.z;

        if (!terrainProvider.isInBounds(wx, wz)) {
            return Optional.empty();
        }

        return Optional.of(new Vector3f(wx, terrainProvider.getHeight(wx, wz), wz));
    }

    private Unit findUnitFromGeometry(com.jme3.scene.Geometry geom) {
        Unit unit = unitRegistry.findByEntityId(geom).orElse(null);
        if (unit == null && geom.getParent() != null) {
            unit = unitRegistry.findByEntityId(geom.getParent()).orElse(null);
        }
        return unit;
    }
}
