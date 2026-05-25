package com.jrts.movement;

import com.jrts.scene.TerrainHeightProvider;
import com.jrts.unit.Unit;
import com.jrts.unit.UnitFlags;
import com.jme3.math.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles terrain collision and Z-clamping for all units every frame.
 * Implements the ground-snapping logic from spec Section 2.
 *
 * Per-frame for each unit:
 *   a. Query terrain height + normal at unit XY
 *   b. If unit Y < terrain height → snap up (ground collision), zero vertical vel
 *   c. If STICK_TO_GROUND and not airborne → always clamp
 *   d. If was airborne and now grounded → detect landing
 *   e. Track airborne state across frames
 */
public class TerrainSnapping {

    private static final Logger log = LoggerFactory.getLogger(TerrainSnapping.class);

    private final TerrainHeightProvider terrain;

    public TerrainSnapping(TerrainHeightProvider terrain) {
        this.terrain = terrain;
        log.info("TerrainSnapping initialized");
    }

    /**
     * Apply ground clamping to a single unit for one frame.
     * Must be called every frame for every unit.
     *
     * @param unit the unit to clamp
     * @return true if a ground collision was detected this frame (landing or penetration)
     */
    public boolean clamp(Unit unit) {
        Vector3f pos = unit.getPosition();
        float groundY = terrain.getHeight(pos.x, pos.z);
        Vector3f normal = terrain.getNormal(pos.x, pos.z);
        float targetY = groundY + unit.getPreferredHeight();

        boolean wasAirborne = unit.wasAirborne();
        boolean isAirborne = unit.isAirborne();

        boolean collided = false;

        if (isAirborne) {
            if (pos.y <= groundY) {
                pos.y = groundY;
                unit.setPosition(pos);
                unit.setWasAirborne(true);
                unit.setGrounded(false);
                collided = true;
                log.trace("Airborne unit {} hit ground at y={}", unit.getId(), groundY);
            } else {
                unit.setWasAirborne(true);
                unit.setGrounded(false);
            }
            return collided;
        }

        if (pos.y < groundY) {
            pos.y = groundY;
            unit.setPosition(pos);
            unit.setGrounded(true);
            collided = true;
            log.trace("Unit {} penetrated terrain, snapped to y={}", unit.getId(), groundY);
        } else if (unit.isStickToGround()) {
            pos.y = targetY;
            unit.setPosition(pos);
            unit.setGrounded(true);
        } else if (pos.y > groundY + 0.1f) {
            unit.setGrounded(false);
        } else {
            unit.setGrounded(true);
        }

        if (wasAirborne && unit.isGrounded() && !isAirborne) {
            collided = true;
            log.trace("Unit {} landed on terrain at y={}", unit.getId(), groundY);
        }

        unit.setWasAirborne(isAirborne && !unit.isGrounded());
        return collided;
    }

    /**
     * Clamp all units in the given registry.
     */
    public void clampAll(com.jrts.unit.UnitRegistry registry) {
        for (Unit unit : registry.allUnits()) {
            clamp(unit);
        }
    }
}
