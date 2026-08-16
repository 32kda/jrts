package com.jrts.movement;

import com.jrts.pathfinding.PathfindingTestSupport;
import com.jrts.unit.Unit;
import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LocalAvoidanceTest {

    private final LocalAvoidance avoidance = new LocalAvoidance(3f);

    @Test
    void overlappingUnitsAreSeparated() {
        Unit a = PathfindingTestSupport.createUnit(1, 0f, 0f, 0f);
        Unit b = PathfindingTestSupport.createUnit(2, 1f, 0f, 0f);

        float before = a.getPosition().distance(b.getPosition());
        avoidance.separate(List.of(a, b));
        float after = a.getPosition().distance(b.getPosition());

        assertTrue(after > before, "units should move apart");
    }

    @Test
    void coincidentUnitsAreSeparated() {
        Unit a = PathfindingTestSupport.createUnit(1, 0f, 0f, 0f);
        Unit b = PathfindingTestSupport.createUnit(2, 0f, 0f, 0f);

        avoidance.separate(List.of(a, b));

        assertTrue(a.getPosition().distance(b.getPosition()) > 0.001f,
                "coincident units must not stay at the same point");
    }

    @Test
    void distantUnitsAreUnaffected() {
        Unit a = PathfindingTestSupport.createUnit(1, 0f, 0f, 0f);
        Unit b = PathfindingTestSupport.createUnit(2, 100f, 0f, 0f);

        Vector3f before = b.getPosition().clone();
        avoidance.separate(List.of(a, b));

        assertEquals(before, b.getPosition());
    }

    @Test
    void emptyListIsNoOp() {
        avoidance.separate(List.of());
    }
}
