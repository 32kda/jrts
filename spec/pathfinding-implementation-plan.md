# Pathfinding & Collision Implementation Plan

Status: Finalized — clean-room implementation (no GPL code; `AIPathfind.cpp` used as conceptual reference only).

## 1. Goals

- **Scale**: C&C Tiberian Sun / Generals-like unit counts — hundreds, up to ~500 moving units.
- **Terrain**: heightfield with 3–4 height levels, ramps (narrow/wide, arbitrary shape), water,
  cliffs/impassable rocks. Until real terrain arrives, water/rocks are *emulated* by
  cube/cylinder obstacles — the search algorithms must be identical whether a blocked cell comes
  from an obstacle or from water/cliff.
- **Bridges**: not now, but the design must make them additive later (layers).

## 2. Locked decisions

1. **Hierarchical pathfinding = HPA\*** (Botea et al. 2004, published/clean-room), not a
   Generals-style zone search.
2. **cellSize = 1.0 world unit** default (overrides the `10.0` placeholder). Clearance-aware:
   a unit occupies `ceil(radius / cellSize)` cells (tank radius 1.5 → 2 cells).
3. **Movement**: keep `MovementController` (turn+move waypoint follower); add
   `SteeringController` (velocity + flow-field sampling) for field-guided units.
4. **Async** time-sliced path request queue = milestone M3.

## 3. Architecture

Single Gradle module, package-level separation (a future multi-module split is possible).

```
com.jrts.pathfinding        (core — no JME types)
  GridCell, CellType, SurfaceMask, TraversalProfile
  Grid, PathResult, AStarPathfinder, PathSmoother
  ConnectedComponents, HpaPathfinder (M1)
  FlowField, FlowFieldService (M2), PathRequestQueue (M3)
com.jrts.movement           (JME adapters + runtime movement)
  NavigationService, AStarNavigation, FlowFieldNavigation (M2)
  MovementController (waypoint follower), SteeringController (M2)
  LocalAvoidance, TerrainSnapping
com.jrts.scene
  TerrainHeightProvider, HeightmapTerrainProvider (M4)
  TerrainGridBaker, ObstacleRenderer, TestMap
com.jrts.tactical           (deferred: nearest-free-cell, fire-position)
```

**Key seam**: `TerrainGridBaker` converts *any* terrain source (flat, heightmap, obstacles, water,
cliff) into cell metadata. Search algorithms read only cell metadata — so "obstacle vs water vs
cliff" are indistinguishable. This is what makes obstacle emulation valid for testing.

## 4. Data model

- **`CellType`**: `CLEAR, WATER, CLIFF, RAMP, OBSTACLE, IMPASSABLE` (+ `RUBBLE` reserved).
- **`SurfaceMask`** (int bitmask): `GROUND=1, WATER=2, CLIFF=4, AIR=8, RUBBLE=16`. A cell *provides*
  a surface; a unit *traverses* a subset. Traversal iff `(unit.surfaces & cell.surface) != 0`.
- **`TraversalProfile`**: `record(int surfaces, float maxClimb, int clearanceCells)`.
- **`Grid`** (per-layer): `originX/Z`, `width/height`, `cellSize`, `layer` (0 = ground), and per-cell
  arrays `type[]`, `height[]`, `cost[]`, `componentId[]`. World↔cell + traversal queries.
- **Ramps** = height transitions within layer 0 (no extra layer): a cell edge is walkable when
  `|Δheight| ≤ maxClimb` and not `CLIFF`; ramp cells carry a small slope cost. **Bridges** (M4+) add
  stacked `Grid` layers with connect-cells.

## 5. Algorithms

1. **Connected components**: BFS/union-find over cells traversable for a given surface mask;
   O(1) reachability rejection. Rebuilt on obstacle/terrain change.
2. **Fine A\***: 8-dir + octile + corner-cut prevention, extended with clearance (footprint),
   surface mask, slope/cliff edge checks, and per-cell cost. Deterministic ties.
3. **HPA\*** (M1): cluster graph (8×8 cells), entrance nodes, intra-cluster paths, lazy invalidation.
4. **Path smoothing**: clearance/surface-aware string-pulling (LOS).
5. **Flow fields** (M2): cost → integration (Dijkstra) → direction, versioned cache.
6. **Async queue** (M3): priority/cancel/expiry, per-frame cell budget, results on update thread.
7. **Local avoidance**: positional separation + uniform-grid spatial hash (M3).

## 6. Terrain baking

`TerrainGridBaker.bake(TerrainHeightProvider, cellSize, obstacles) → Grid`:
- water (`isWater`) → `WATER`; steep gradient → `CLIFF`; ramp (`|Δheight| ≤ maxRampClimb`) → `RAMP` + slope cost;
- obstacle footprints → `OBSTACLE`; compute ground connected components.

Consumes the existing `TerrainHeightProvider` (already has `getHeight/isWater/getGradient`), so
`HeightmapTerrainProvider` (M4) drops in with no search-code changes.

## 7. Movement integration

- `MovementController` (waypoint follower) consumes per-unit A* paths.
- `SteeringController` (M2) samples a flow field → velocity; combined with `LocalAvoidance`.
- Stuck detection: unit stall ≥ N frames → re-issue a path request (M3).

## 8. Milestones

| # | Scope | Acceptance criteria |
|---|---|---|
| **M0** | Terrain/surfaces, clearance A*, components, baker | Water/cliff/ramp/obstacle all block ground units identically; amphibious/hover cross water; components reject unreachable goals; existing tests green |
| **M1** | HPA\* | Individual path latency low on large maps; results == fine A* (deterministic) |
| **M2** | Flow fields + steering | 100+ units to one destination in bounded time; field invariants hold |
| **M3** | Async queue + spatial-hash avoidance | No frame hitch ordering 200+ units; 500-unit stress within NFR budgets |
| **M4** | Real terrain (heightmap/ramps), bridges | Units climb ramps, avoid cliffs/water; bridges via layers |

Bridges remain a documented extension point (the `Grid.layer` seam from M0) rather than a milestone.

## 9. Testing strategy

- **Unit** (ASCII fixtures + `@TempDir`): components; surface/slope/clearance traversal; A* cases;
  smoothing; (M1) HPA\* ≡ fine A*; (M2) flow-field invariants; (M3) queue + hash.
- **Integration** (headless simulation): single unit + groups over flat terrain and cube/cylinder/wall
  obstacles (emulating water/rocks); multi-level ramp maps (M4); determinism; stuck re-path.
- **Perf smoke** (non-blocking): A* < 1 ms, flow field < 5 ms, 200-unit avoidance < 3 ms,
  500-unit frame budget.
- **Golden files**: a few canonical maps/paths for regression.
