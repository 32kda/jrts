package com.jrts.movement;

import com.jrts.scene.TerrainHeightProvider;
import com.jrts.unit.Unit;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Per-frame movement update for ONE unit.
 * Called by the main update loop for each unit with active waypoints.
 *
 * Per-frame logic:
 *   a. Compute desired velocity toward next waypoint
 *   b. Accelerate/Decelerate toward maxSpeed
 *   c. Turn body toward desired heading (maxTurnRate clamped)
 *   d. If within closeEnoughDist → advance to next waypoint
 *   e. Update world position, clamped to terrain height
 *
 * Not a God Class — operates on exactly ONE unit per update call.
 * Depends on TerrainHeightProvider for ground clamping.
 */
public class MovementController {

    private static final Logger log = LoggerFactory.getLogger(MovementController.class);

    private static final float CLOSE_ENOUGH = 0.5f;

    private final TerrainHeightProvider terrain;
    private final TerrainSnapping terrainSnapping;

    public MovementController(TerrainHeightProvider terrain) {
        this.terrain = terrain;
        this.terrainSnapping = new TerrainSnapping(terrain);
        log.info("MovementController initialized with terrain provider");
    }

    /**
     * @param unit the unit to move
     * @param tpf  time per frame (seconds)
     * @return true if still moving (more waypoints remain), false if arrived
     */
    public boolean update(Unit unit, float tpf) {
        List<Vector3f> waypoints = unit.getWaypoints();
        if (waypoints == null || waypoints.isEmpty()) {
            return false;
        }

        float maxSpeed = unit.getConfig().stats().speed();
        float turnRate = unit.getConfig().stats().rot() * FastMath.DEG_TO_RAD;
        Vector3f target = waypoints.get(0);
        Vector3f pos = unit.getPosition().clone();
        Vector3f toTarget = target.subtract(pos);
        float dist = toTarget.length();

        if (dist < CLOSE_ENOUGH) {
            waypoints.remove(0);
            log.trace("Unit {} arrived at waypoint, {} remaining", unit.getId(),
                    waypoints.size());
            return !waypoints.isEmpty();
        }

        float desiredYaw = (float) Math.atan2(toTarget.x, toTarget.z);

        float yawDiff = normalizeAngle(desiredYaw - unit.getBodyYaw());
        float step = Math.min(Math.abs(yawDiff), turnRate * tpf);
        unit.setBodyYaw(unit.getBodyYaw() + Math.signum(yawDiff) * step);

        if (Math.abs(yawDiff) < 0.1f) {
            float speed = Math.min(maxSpeed, dist / tpf);
            Vector3f forward = new Vector3f(
                    (float) Math.sin(unit.getBodyYaw()),
                    0,
                    (float) Math.cos(unit.getBodyYaw())
            ).multLocal(speed * tpf);
            pos.addLocal(forward);
            unit.setPosition(pos);
            terrainSnapping.clamp(unit);
        }

        return true;
    }

    private static float normalizeAngle(float rad) {
        while (rad > FastMath.PI) {
            rad -= FastMath.TWO_PI;
        }
        while (rad < -FastMath.PI) {
            rad += FastMath.TWO_PI;
        }
        return rad;
    }
}
