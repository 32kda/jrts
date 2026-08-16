package com.jrts.movement;

import com.jrts.pathfinding.PathfindingTestSupport;
import com.jrts.scene.FlatTerrainHeightProvider;
import com.jrts.scene.TerrainHeightProvider;
import com.jrts.unit.Unit;
import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the Composite-pattern movement control advances a unit along its waypoints via
 * JME's scene-graph update traversal (mirroring the real game loop: logical then geometric).
 */
class MovementControlTest {

    private static final float TPF = 1f / 60f;

    @Test
    void controlMovesUnitAlongWaypoints() {
        TerrainHeightProvider terrain = new FlatTerrainHeightProvider(0f, 100f);
        Unit unit = PathfindingTestSupport.createUnit(1, 0f, 0f, 0f);
        unit.getSpatial().addControl(new MovementControl(unit, terrain));
        unit.setWaypoints(new ArrayList<>(List.of(new Vector3f(0f, 0f, 10f))));

        for (int i = 0; i < 120; i++) {
            unit.getSpatial().updateLogicalState(TPF);
            unit.getSpatial().updateGeometricState();
        }

        assertTrue(unit.getPosition().z > 0f, "unit should have advanced toward the waypoint");
    }

    @Test
    void controlStopsWhenWaypointsExhausted() {
        TerrainHeightProvider terrain = new FlatTerrainHeightProvider(0f, 100f);
        Unit unit = PathfindingTestSupport.createUnit(1, 0f, 0f, 0f);
        unit.getSpatial().addControl(new MovementControl(unit, terrain));

        // No waypoints: the control must be a no-op.
        for (int i = 0; i < 5; i++) {
            unit.getSpatial().updateLogicalState(TPF);
        }
        assertEquals(0f, unit.getPosition().z, 0.001f);
    }
}
