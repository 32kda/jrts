package com.jrts.movement;

import com.jrts.pathfinding.AStarPathfinder;
import com.jrts.pathfinding.Grid;
import com.jrts.pathfinding.HpaPathfinder;
import com.jrts.pathfinding.Obstacle;
import com.jrts.pathfinding.PathSmoother;
import com.jrts.pathfinding.PathfindingTestSupport;
import com.jrts.pathfinding.SurfaceMask;
import com.jrts.pathfinding.TraversalProfile;
import com.jrts.scene.FlatTerrainHeightProvider;
import com.jrts.scene.TerrainHeightProvider;
import com.jrts.unit.Unit;
import com.jme3.math.Vector3f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end movement simulation: units navigate from A to B on flat terrain and
 * around cube/cylinder/wall obstacles, and must reach their destination without
 * getting stuck.
 */
class PathfindingIntegrationTest {

    private static final float TPF = 1f / 30f;
    private static final float ARRIVE_DIST = 2.0f;
    private static final int MAX_STEPS = 20000;
    private static final TraversalProfile TANK =
            new TraversalProfile(SurfaceMask.GROUND, 1f, 2);

    private TerrainHeightProvider terrain;
    private Grid grid;
    private AStarNavigation navigation;
    private MovementController movementController;
    private LocalAvoidance localAvoidance;
    private TerrainSnapping terrainSnapping;

    @BeforeEach
    void setUp() {
        terrain = new FlatTerrainHeightProvider(0f, 100f);
        grid = Grid.fromBounds(-100f, -100f, 100f, 100f, 1f);
        navigation = new AStarNavigation(grid, new AStarPathfinder(), new PathSmoother(),
                terrain, TANK);
        movementController = new MovementController(terrain);
        localAvoidance = new LocalAvoidance(3f);
        terrainSnapping = new TerrainSnapping(terrain);
    }

    @Test
    void singleUnitReachesDestinationOnFlatTerrain() {
        Unit unit = PathfindingTestSupport.createUnit(1, 0f, 0f, 0f);
        Vector3f dest = new Vector3f(30f, 0f, 0f);
        simulate(List.of(unit), List.of(dest));
        assertTrue(unit.getPosition().distance(dest) < ARRIVE_DIST);
    }

    @Test
    void singleUnitReachesDestinationAroundCube() {
        Obstacle.box(15f, 0f, 3f, 3f).markBlocked(grid, 0f);
        Unit unit = PathfindingTestSupport.createUnit(1, 0f, 0f, 0f);
        Vector3f dest = new Vector3f(30f, 0f, 0f);
        simulate(List.of(unit), List.of(dest));
        assertTrue(unit.getPosition().distance(dest) < ARRIVE_DIST);
    }

    @Test
    void singleUnitReachesDestinationAroundCylinder() {
        Obstacle.cylinder(15f, 0f, 3f).markBlocked(grid, 0f);
        Unit unit = PathfindingTestSupport.createUnit(1, 0f, 0f, 0f);
        Vector3f dest = new Vector3f(30f, 0f, 0f);
        simulate(List.of(unit), List.of(dest));
        assertTrue(unit.getPosition().distance(dest) < ARRIVE_DIST);
    }

    @Test
    void singleUnitReachesDestinationAroundWall() {
        Obstacle.box(15f, 0f, 0.5f, 8f).markBlocked(grid, 0f);
        Unit unit = PathfindingTestSupport.createUnit(1, 0f, 0f, 0f);
        Vector3f dest = new Vector3f(30f, 0f, 0f);
        simulate(List.of(unit), List.of(dest));
        assertTrue(unit.getPosition().distance(dest) < ARRIVE_DIST);
    }

    @Test
    void groupOfUnitsReachesDestinationAroundObstacles() {
        Obstacle.box(20f, 0f, 5f, 5f).markBlocked(grid, 0f);
        Obstacle.cylinder(30f, -12f, 3f).markBlocked(grid, 0f);
        Obstacle.cylinder(30f, 12f, 3f).markBlocked(grid, 0f);

        List<Unit> units = new ArrayList<>();
        List<Vector3f> destinations = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            float sx = (i % 5) * 3f - 6f;
            float sz = (i / 5) * 3f - 1.5f;
            units.add(PathfindingTestSupport.createUnit(i + 1, sx, 0f, sz));

            float dx = 40f + (i % 5) * 3f - 6f;
            float dz = (i / 5) * 3f - 1.5f;
            destinations.add(new Vector3f(dx, 0f, dz));
        }

        simulate(units, destinations);

        for (int i = 0; i < units.size(); i++) {
            assertTrue(units.get(i).getPosition().distance(destinations.get(i)) < ARRIVE_DIST,
                    "unit " + (i + 1) + " did not reach its destination");
        }
    }

    @Test
    void groupReachesDestinationAroundObstaclesWithHierarchicalPathfinder() {
        navigation = new AStarNavigation(grid, new HpaPathfinder(), new PathSmoother(),
                terrain, TANK);

        Obstacle.box(20f, 0f, 5f, 5f).markBlocked(grid, 0f);
        Obstacle.cylinder(30f, -12f, 3f).markBlocked(grid, 0f);
        Obstacle.cylinder(30f, 12f, 3f).markBlocked(grid, 0f);

        List<Unit> units = new ArrayList<>();
        List<Vector3f> destinations = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            float sx = (i % 5) * 3f - 6f;
            float sz = (i / 5) * 3f - 1.5f;
            units.add(PathfindingTestSupport.createUnit(i + 1, sx, 0f, sz));
            destinations.add(new Vector3f(40f + (i % 5) * 3f - 6f, 0f, (i / 5) * 3f - 1.5f));
        }

        simulate(units, destinations);

        for (int i = 0; i < units.size(); i++) {
            assertTrue(units.get(i).getPosition().distance(destinations.get(i)) < ARRIVE_DIST,
                    "unit " + (i + 1) + " did not reach its destination");
        }
    }

    private void simulate(List<Unit> units, List<Vector3f> destinations) {
        for (int i = 0; i < units.size(); i++) {
            List<Vector3f> path = navigation.computePath(units.get(i).getPosition(), destinations.get(i));
            assertFalse(path.isEmpty(), "path must never be empty");
            units.get(i).setWaypoints(new ArrayList<>(path));
        }

        boolean[] arrived = new boolean[units.size()];
        int remaining = units.size();
        for (int step = 0; step < MAX_STEPS && remaining > 0; step++) {
            List<Unit> active = new ArrayList<>();
            for (int i = 0; i < units.size(); i++) {
                if (arrived[i]) {
                    continue;
                }
                Unit unit = units.get(i);
                boolean stillMoving = movementController.update(unit, TPF);
                if (!stillMoving || unit.getPosition().distance(destinations.get(i)) < ARRIVE_DIST) {
                    unit.setWaypoints(Collections.emptyList());
                    arrived[i] = true;
                    remaining--;
                } else {
                    active.add(unit);
                }
            }
            localAvoidance.separate(active);
            for (Unit unit : active) {
                terrainSnapping.clamp(unit);
            }
        }

        assertEquals(0, remaining, "not all units arrived within the step budget");
    }
}
