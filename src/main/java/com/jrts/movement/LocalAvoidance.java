package com.jrts.movement;

import com.jrts.unit.Unit;
import com.jme3.math.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Minimal local avoidance: positional separation steering in the XZ plane.
 *
 * After movement, overlapping units are pushed apart so they do not permanently stack.
 * Applied to groups of units sharing a destination or traversing a corridor.
 */
public class LocalAvoidance {

    private static final Logger log = LoggerFactory.getLogger(LocalAvoidance.class);

    private static final float EPS = 1e-4f;

    private final float separationRadius;

    public LocalAvoidance(float separationRadius) {
        this.separationRadius = separationRadius;
        log.info("LocalAvoidance initialized with separationRadius={}", separationRadius);
    }

    /**
     * Resolve overlaps between the given units. Each overlapping pair is pushed apart
     * by half the overlap along the line connecting them.
     */
    public void separate(List<Unit> units) {
        for (int i = 0; i < units.size(); i++) {
            for (int j = i + 1; j < units.size(); j++) {
                resolve(units.get(i), units.get(j));
            }
        }
    }

    private void resolve(Unit a, Unit b) {
        Vector3f pa = a.getPosition();
        Vector3f pb = b.getPosition();
        float dx = pb.x - pa.x;
        float dz = pb.z - pa.z;
        float dist = (float) Math.sqrt(dx * dx + dz * dz);
        if (dist >= separationRadius) {
            return;
        }
        float overlap = separationRadius - dist;
        float push = overlap * 0.5f;

        if (dist < EPS) {
            dx = 1f;
            dz = 0f;
        } else {
            dx /= dist;
            dz /= dist;
        }

        a.setPosition(new Vector3f(pa.x - dx * push, pa.y, pa.z - dz * push));
        b.setPosition(new Vector3f(pb.x + dx * push, pb.y, pb.z + dz * push));
    }
}
