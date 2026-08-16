# jMonkeyEngine RTS Pathfinding and Tactical Movement Specification

Status: Draft  
Target engine: jMonkeyEngine  
License goal: Avoid GPL/AGPL contamination; use permissively licensed code and original implementation.

---

## 1. Overview

This document specifies a pathfinding and tactical movement system for a jMonkeyEngine-based real-time strategy game.

The system must support:

- Many units moving simultaneously.
- Finding a path to a free cell nearest to a given point.
- Finding a path to a position from which a unit can open fire at a given point.
- Grid-based and/or navmesh-based pathability.
- Group movement using scalable techniques such as flow fields.
- Local avoidance to prevent units from overlapping.
- Licensing hygiene suitable for a permissively licensed or proprietary project.

The pathfinding system should be engine-independent where possible. jMonkeyEngine should be used mainly for rendering, input, scene management, and optional spatial queries.

---

## 2. Goals

### 2.1 Functional Goals

The system should support:

1. Individual unit pathfinding.
2. Squad or group pathfinding.
3. Movement commands to a point.
4. Movement to the nearest reachable free cell around a point.
5. Movement to a firing position for a target point or target entity.
6. Attack-move behavior.
7. Dynamic obstacles such as buildings and temporary blockers.
8. Many units moving at the same time without per-unit full A\* every frame.

### 2.2 Technical Goals

The system should be:

- Deterministic where practical.
- Efficient enough for real-time strategy gameplay.
- Decoupled from rendering.
- Testable without starting the full jMonkeyEngine visual pipeline.
- Based on permissively licensed code.
- Easy to debug using grid, path, and flow-field visualization.

---

## 3. Non-Goals

This specification does not require:

- Full physics simulation for all units.
- Perfect pathfinding for every possible edge case.
- Navmesh runtime rebuilding for every moving unit.
- Pixel-perfect movement.
- Fully realistic crowd simulation.

The system may use approximations when they improve scalability and gameplay feel.

---

## 4. Licensing Constraints

The project should avoid GPL-related complaints.

### 4.1 General Policy

Allowed or low-risk licenses:

- MIT
- BSD-2-Clause
- BSD-3-Clause
- ISC
- Zlib
- Apache-2.0, provided license and notice files are preserved

Use with caution:

- LGPL
- EPL
- MPL
- CC-BY-SA for code

Avoid copying or linking without legal review:

- GPL-2.0
- GPL-3.0
- AGPL
- code from GPL strategy games
- code from unclear-license repositories

### 4.2 Implementation Policy

- Do not copy GPL code into the repository.
- Do not closely translate GPL code into this project.
- Prefer original clean-room implementation.
- Use permissively licensed libraries only when necessary.
- Keep third-party license files in a `licenses/` or `THIRD-PARTY-LICENSES` directory.
- If Apache-2.0 code is used, preserve notices and document material changes if required.

### 4.3 Engine Licensing

jMonkeyEngine is BSD-3-Clause.

This is generally compatible with a permissively licensed game, but the project should retain JME license notices.

### 4.4 Optional Libraries

Examples:

| Library | Potential use | Licensing note |
|---|---|---|
| jMonkeyEngine | Rendering/input/scene | BSD-3-Clause |
| Recast4j | Navmesh generation/pathfinding | Verify current license; use only if permissive |
| libGDX AI | Reference for A\*/steering | Apache-2.0; do not copy unless complying |
| JGraphT | Graph algorithms | LGPL/EPL-ish; avoid if strict permissive policy |
| GPL Java strategy games | Reference only | Do not copy code |

---

## 5. Target Environment

Assumed environment:

- Java 17+
- jMonkeyEngine 3.x
- Desktop target initially
- Real-time strategy gameplay
- Potentially hundreds of moving units
- Map represented as terrain plus static and dynamic obstacles

The pathfinding module should not require a running graphical context for core logic tests.

---

## 6. Core Concepts

### 6.1 Grid World

The primary pathfinding representation is a 2D grid over the XZ plane.

```text
world.x -> grid.x
world.z -> grid.z
world.y -> height / line-of-sight only
```

Each grid cell stores:

- static blocked state
- dynamic blocked state
- movement cost
- connected component ID
- optional tactical metadata

### 6.2 Static Obstacles

Static obstacles include:

- terrain walls
- cliffs
- water, if non-walkable
- buildings
- rocks
- permanent blockers

Static obstacles should be baked into the pathfinding grid or navmesh.

### 6.3 Dynamic Obstacles

Dynamic obstacles include:

- temporary blockers
- destroyed or constructed buildings
- area hazards
- reserved formation cells
- unit congestion cost

Moving units should usually not be treated as hard grid blockers for global pathfinding. Instead, use local avoidance.

### 6.4 Connected Components

The static walkability map should be divided into connected components.

This allows fast rejection of unreachable cells:

```text
unit.cell.componentId == candidate.cell.componentId
```

Dynamic obstacles may require local revalidation.

### 6.5 Flow Field

A flow field is a direction field that guides many units toward a target.

It is generated from:

1. cost field
2. distance/integration field
3. direction field

Flow fields are preferred for large group movement.

---

## 7. Functional Requirements

### FR-1: Pathability Query

The system must answer:

```text
Is this cell walkable?
```

Inputs:

- grid cell or world position

Outputs:

- boolean walkable
- optional movement cost

Requirements:

- Must consider static blockers.
- Must consider dynamic blockers where appropriate.
- Must be fast enough to query during movement and tactical searches.

---

### FR-2: Find Path to Point

The system must find a path from a unit position to a target point.

Inputs:

- unit position or start cell
- target world position
- optional pathfinding filter

Outputs:

- path result:
  - success/failure
  - sequence of waypoints or cells
  - final world position
  - path cost

Behavior:

- If target cell is blocked, the system may attempt to find a nearby free reachable cell.
- If target is unreachable, return failure.
- Paths should avoid blocked cells.
- Diagonal movement should respect corner-cutting rules.

---

### FR-3: Find Path to Nearest Free Cell Near Point

The system must find a path to a free cell nearest to a given point.

Inputs:

- unit reference or start cell
- desired world point
- maximum search radius
- optional scoring function

Outputs:

- selected free cell
- path to selected cell
- failure if no suitable cell exists

Requirements:

- The selected cell must be free.
- The selected cell must be reachable from the unit.
- The selected cell should be near the desired point.
- Search should prefer closer cells first.
- If multiple cells are equally good, behavior should be deterministic.
- The query should support dynamic blockers.
- The query should not return a cell on another disconnected island unless reachable by allowed movement.

Suggested search order:

```text
1. Check target cell.
2. Search outward in rings around target cell.
3. Filter by free and reachable.
4. Score candidates.
5. Return best candidate.
```

---

### FR-4: Find Path to Open Fire at Point

The system must find a path to a position from which the unit can fire at a given point.

Inputs:

- unit reference or start cell
- target world point
- weapon specification:
  - min range
  - max range
  - line-of-sight requirement
  - preferred range, optional
  - firing arc, optional

Outputs:

- firing cell
- path to firing cell
- failure if no valid firing position exists

Requirements:

Candidate firing cell must satisfy:

- free cell
- reachable from unit
- distance to target <= max range
- distance to target >= min range
- line of sight to target, if required
- valid terrain
- not inside a prohibited area

Candidate scoring may include:

```text
score =
    path cost from unit
  + distance-to-preferred-range penalty
  + danger cost
  + occupancy cost
  + angle penalty
```

Deterministic tie-breaking is required.

---

### FR-5: Group Movement

The system must support moving many units to a shared target area.

Inputs:

- group of units
- destination point
- group options

Outputs:

- movement commands
- flow field handle or squad paths

Recommended behavior:

- Use one flow field for many units moving to the same destination.
- Use formation slots or free-cell allocation near the destination.
- Avoid giving every unit a separate full A\* path when possible.

---

### FR-6: Attack-Move Behavior

The system must support attack-move commands.

Inputs:

- group or unit
- target point or target area
- weapon specification

Behavior:

1. Units move toward target using flow field or path.
2. Units stop and fire when:
   - target is within range
   - line of sight exists
   - unit is not blocked
3. If target becomes invalid, units resume movement or re-evaluate.

---

### FR-7: Local Avoidance

The system must keep units from permanently overlapping.

Requirements:

- Units should avoid nearby units.
- Units should not be pushed through hard static obstacles.
- Avoidance should respect max speed and max acceleration.
- System should remain stable in dense groups.

Possible implementations:

- separation steering
- velocity obstacles
- simplified ORCA/RVO-like avoidance

Licensing note:

- Avoid copying unclear-license ORCA/RVO code.
- Prefer original implementation or clearly permissive code.

---

### FR-8: Asynchronous Path Requests

Pathfinding should not block the render thread.

Requirements:

- Support queued path requests.
- Support priorities.
- Support cancellation.
- Support expiration of stale requests.
- Results should be applied on the game update thread.

---

### FR-9: Debug Visualization

The system should support visual debugging.

Visualizations may include:

- grid cells
- blocked cells
- path cells
- flow field arrows
- firing range rings
- line-of-sight checks
- candidate cells
- selected destination cells
- connected component colors

---

## 8. Non-Functional Requirements

### NFR-1: Performance

Example target budgets, adjustable per project:

| Operation | Target budget |
|---|---:|
| Single A\* query on medium map | < 1 ms typical |
| Nearest free cell query | < 0.5 ms typical |
| Fire position query | < 1 ms typical |
| Flow field update | < 5 ms typical |
| Avoidance update for 200 units | < 3 ms typical |
| Frame time impact for 500 units | playable at target FPS |

The system should avoid per-frame allocations in hot paths.

---

### NFR-2: Scalability

The system should support:

- Small groups: 1–10 units
- Medium groups: 10–50 units
- Large groups: 50–200 units
- Stress tests: 500+ units, possibly with reduced fidelity

Group movement should prefer flow fields or hierarchical techniques over per-unit A\*.

---

### NFR-3: Determinism

Given:

- same map
- same obstacles
- same start and target
- same query parameters
- same random seed, if any

the system should produce the same result.

Floating-point-only cosmetic movement does not need perfect determinism, but pathfinding and tactical queries should be deterministic enough for replay and testing.

---

### NFR-4: Thread Safety

The pathfinding module should support:

- read-only access to static grid data from worker threads
- controlled updates to dynamic grid data
- thread-safe request queues
- safe publication of path results

---

### NFR-5: Maintainability

The system should be modular:

```text
rts-core
rts-pathfinding
rts-tactical
rts-jme-adapters
```

Core pathfinding should not depend directly on jMonkeyEngine scene classes where avoidable.

---

## 9. Recommended Architecture

### 9.1 Module Layout

```text
rts-core
    common math, interfaces, grid cell types

rts-pathfinding
    grid world
    A*
    JPS, optional
    flow fields
    connected components

rts-tactical
    nearest free cell query
    firing position query
    cover query, optional
    formation slot query, optional

rts-jme-adapters
    JME coordinate conversion
    JME picking/raycast adapters
    debug draw helpers

rts-game
    units
    commands
    simulation
    rendering
```

### 9.2 Layering

```text
Game commands
    |
    v
Tactical query layer
    |
    v
Pathfinding layer
    |
    v
Grid/navmesh data layer
    |
    v
JME adapter layer
```

---

## 10. Algorithm Recommendations

### 10.1 Individual Pathfinding

Use A\* for:

- individual units
- small squads
- special commands
- fallback when flow fields are not suitable

Optional optimization:

- Jump Point Search for uniform-cost grids

A\* implementation recommendations:

- use integer cell IDs
- use primitive arrays
- use custom binary heap
- avoid object allocation in search loop
- support 4-direction or 8-direction movement
- use octile heuristic for 8-direction movement

---

### 10.2 Nearest Free Cell Query

Recommended algorithm:

```text
targetCell = worldToCell(desiredPoint)

if targetCell is free and reachable:
    return targetCell

for radius in 1..maxRadius:
    for cell in ring(targetCell, radius):
        if free(cell) and reachable(cell):
            candidates.add(cell)

    if candidates not empty:
        return best candidate by score
```

Scoring:

```text
score =
    ringDistance * weightRing
  + pathCostFromUnit * weightPath
  + dynamicOccupancyPenalty * weightOccupancy
```

If path cost is too expensive to compute for every candidate:

1. Use connected component for reachability.
2. Select a small number of best nearby candidates.
3. Run A\* only for those candidates.

---

### 10.3 Fire Position Query

Recommended algorithm:

```text
candidates = cells within max weapon range of target point

filter candidates:
    free
    reachable
    distance >= minRange
    distance <= maxRange
    line of sight, if required

score candidates:
    path cost
    range preference
    danger
    occupancy
    angle

return best candidate
```

Optimization:

- Cache firing candidates per target point.
- Cache line-of-sight results where practical.
- Use coarse candidate sampling first, then refine.

---

### 10.4 Flow Fields

Recommended generation:

```text
1. Build cost field.
2. Initialize destination cells with cost 0.
3. Run Dijkstra/BFS over walkable cells.
4. Produce integration field.
5. For each cell, choose neighbor with lowest cost.
6. Convert neighbor choice into direction vector.
```

Units sample the flow field:

```text
direction = flowField.sample(unit.position)
velocity = direction * speed
```

Add local avoidance afterward.

---

### 10.5 Local Avoidance

Minimum viable avoidance:

```text
for each nearby neighbor:
    add separation force away from neighbor

clamp force
integrate velocity
resolve collisions against static blockers
```

Higher-quality avoidance:

- velocity obstacles
- reciprocal velocity obstacles
- simplified ORCA

For many units, avoidance should be spatially accelerated using:

- uniform grid
- spatial hash
- quadtree

---

## 11. Proposed API Sketch

### 11.1 Grid

```java
public interface RtsGrid {
    int getWidth();
    int getHeight();
    float getCellSize();

    GridCell worldToCell(Vector3f worldPosition);
    Vector3f cellToWorld(GridCell cell);

    boolean isFree(GridCell cell);
    boolean isBlocked(GridCell cell);
    float getCost(GridCell cell);

    int getConnectedComponent(GridCell cell);
    boolean hasLineOfSight(GridCell from, GridCell to);
}
```

### 11.2 Pathfinder

```java
public interface RtsPathfinder {
    PathResult findPath(UnitRef unit, GridCell goal);

    PathResult findPathToNearestFreeCell(
        UnitRef unit,
        Vector3f desiredPoint,
        float maxSearchRadius
    );

    PathResult findPathToFirePosition(
        UnitRef unit,
        Vector3f targetPoint,
        WeaponSpec weapon
    );
}
```

### 11.3 Tactical Query Service

```java
public interface TacticalQueryService {
    GridCell findNearestFreeCell(
        UnitRef unit,
        Vector3f desiredPoint,
        float maxRadius
    );

    GridCell findBestFiringCell(
        UnitRef unit,
        Vector3f targetPoint,
        WeaponSpec weapon
    );
}
```

### 11.4 Flow Fields

```java
public interface FlowFieldService {
    FlowField getOrCreateMoveField(Vector3f destination);

    FlowField getOrCreateAttackField(
        Vector3f targetPoint,
        WeaponSpec weapon
    );
}
```

### 11.5 Flow Field

```java
public interface FlowField {
    boolean isReady();
    int getVersion();
    Vector2f sampleDirection(Vector3f worldPosition);
}
```

---

## 12. Data Structures

### 12.1 Grid Data

```java
class GridData {
    int width;
    int height;
    float cellSize;

    byte[] staticFlags;
    byte[] dynamicFlags;
    float[] cost;
    int[] connectedComponent;
}
```

### 12.2 Search Data

```java
class AStarSearchData {
    int[] cameFrom;
    float[] gScore;
    float[] fScore;
    byte[] searchState;
    int[] openHeap;
}
```

### 12.3 Flow Field Data

```java
class FlowFieldData {
    int[] integration;
    short[] directionX;
    short[] directionZ;
    int version;
}
```

---

## 13. Edge Cases

The system must handle:

1. Start cell blocked.
2. Target cell blocked.
3. Target cell unreachable.
4. Target point outside map bounds.
5. Unit already at destination.
6. No free cell within search radius.
7. No firing position within range.
8. Line of sight blocked.
9. Target inside obstacle.
10. Unit trapped by dynamic obstacle.
11. Group destination in narrow choke point.
12. Many units assigned to same small area.
13. Dynamic obstacle appears during movement.
14. Path request becomes stale.
15. Flow field destination becomes blocked.

---

## 14. Acceptance Criteria

### 14.1 Pathfinding

A path result is valid if:

- start cell is valid or adjusted to nearest valid reachable cell.
- end cell is free and reachable.
- no step enters a blocked cell.
- diagonal movement does not illegally cut corners.
- path is connected.
- pathfinding result is deterministic for identical input.

### 14.2 Nearest Free Cell

The nearest-free-cell query is valid if:

- returned cell is free.
- returned cell is reachable.
- returned cell is within max radius.
- no closer valid free cell exists according to defined search order.
- query fails cleanly if no valid cell exists.

### 14.3 Fire Position

The fire-position query is valid if:

- returned cell is free.
- returned cell is reachable.
- target is within weapon range.
- target is outside minimum range, if applicable.
- line-of-sight requirement is satisfied.
- query fails cleanly if no valid position exists.

### 14.4 Flow Fields

A flow field is valid if:

- reachable cells have a distance/integration value.
- directions point toward equal or lower cost.
- blocked cells are not assigned valid directions.
- unreachable cells are marked unreachable.
- units sampling the field make progress toward destination in typical cases.

### 14.5 Group Movement

Group movement is acceptable if:

- most units reach the destination area.
- units do not permanently stack.
- units do not visibly pass through hard obstacles.
- no unit remains stuck forever under normal map conditions.
- movement remains stable in open areas and choke points.

---

# 15. Testing Strategy

This section defines how the system should be tested.

---

## 15.1 Testing Goals

The test strategy should verify:

- correctness
- reachability
- performance
- determinism
- robustness
- license compliance
- integration with jMonkeyEngine adapters
- acceptable gameplay behavior

---

## 15.2 Test Pyramid

Recommended distribution:

```text
Unit tests:            60–70%
Integration tests:     20–25%
Scenario/system tests: 5–10%
Manual/visual tests:   as needed
Performance tests:     scheduled, not necessarily blocking
```

Core pathfinding logic should be testable without starting the full game renderer.

---

## 15.3 Isolate Core From jMonkeyEngine

The pathfinding and tactical modules should depend on interfaces, not directly on JME rendering classes.

Example:

```java
public interface WorldGeometry {
    float getHeight(float x, float z);
    boolean isBlockedArea(float x, float z);
}
```

jME adapters implement these interfaces.

This allows most tests to run as plain JVM unit tests.

---

## 15.4 Recommended Test Frameworks

If your licensing policy allows test-only dependencies:

- JUnit 5
  - License: EPL-2.0
  - Usually acceptable if only used in tests and not distributed
- AssertJ
  - License: Apache-2.0
- Mockito
  - License: MIT
- ArchUnit
  - License: Apache-2.0
  - useful for enforcing module boundaries

If EPL is not acceptable even for tests:

- use TestNG, Apache-2.0
- or use a lightweight custom test runner

For performance testing:

- use JMH only if license policy accepts it
- otherwise use a custom benchmark harness

---

## 15.5 ASCII Map Fixtures

Use small text maps for deterministic tests.

Example:

```text
#####
#S..#
#.#.#
#..G#
#####
```

Legend:

```text
# = blocked
. = free
S = start
G = goal
```

This makes tests readable and easy to maintain.

Additional fixture types:

```text
open field
single obstacle
diagonal corridor
choke point
island map
unreachable goal
target inside wall
range ring around target
LOS blocked map
dynamic obstacle map
```

---

## 15.6 Unit Tests

### Grid Tests

Test:

- world-to-grid conversion
- grid-to-world conversion
- cell bounds clamping
- blocked/free flags
- cost values
- connected components
- dynamic obstacle updates

Example cases:

```text
world position inside cell returns expected cell
negative coordinates handled correctly
out-of-bounds positions handled safely
dynamic block overrides free cell
connected component IDs are stable after rebuild
```

---

### A\* Tests

Test:

- straight-line path
- path around obstacle
- unreachable goal
- start equals goal
- blocked start
- blocked goal
- diagonal movement
- diagonal corner cutting
- weighted terrain
- path cost correctness

Example assertions:

```text
path starts at start cell
path ends at goal cell
path contains no blocked cells
each step is adjacent to previous step
path length is optimal for simple maps
```

---

### Nearest Free Cell Tests

Test:

- target cell already free
- target cell blocked, adjacent free cell selected
- target surrounded by blocked cells
- no free cell within radius
- unreachable free cell is ignored
- dynamic obstacle blocks candidate
- deterministic tie-breaking

Example map:

```text
#####
#.#G#
#S###
#####
```

Expected:

- query should not return unreachable cell behind wall
- query should return nearest reachable free cell if one exists

---

### Fire Position Tests

Test:

- target in range from start, no movement needed
- target out of range, valid firing cell found
- target blocked by LOS obstacle
- min range prevents too-close cells
- max range prevents too-far cells
- no reachable firing position
- candidate scoring deterministic

Example assertions:

```text
returned firing cell is free
returned firing cell is reachable
distance(cell, target) <= maxRange
distance(cell, target) >= minRange
lineOfSight(cell, target) == true
```

---

### Flow Field Tests

Test:

- destination field generation
- blocked cells excluded
- unreachable cells marked unreachable
- directions point downhill
- multiple destinations
- dynamic obstacle invalidation
- field version increments on update

Example invariant:

```text
For every reachable non-destination cell:
    integration[cell] > integration[nextCell]
    direction[cell] points toward nextCell or neighbor with lower integration
```

---

### Local Avoidance Tests

Test:

- two units heading toward each other separate
- units do not overlap beyond allowed radius
- avoidance does not push units through walls
- dense group remains stable
- avoidance respects max force

These can be approximate simulation tests rather than exact.

---

## 15.7 Property-Based and Random Tests

Use randomized but seeded tests to find edge cases.

Generate:

- random maps
- random start/goal pairs
- random obstacle densities
- random weapon ranges
- random group sizes

Assert invariants:

- pathfinder terminates
- no path contains blocked cells
- reachable goals eventually find paths
- unreachable goals fail cleanly
- nearest-free query returns valid cell or failure
- fire-position query returns valid cell or failure
- flow field directions reduce cost

Use fixed seeds for reproducibility.

---

## 15.8 Golden File / Snapshot Tests

For selected maps and queries, store expected results:

```text
test/resources/golden/
    simple-corridor.path.json
    blocked-goal.nearest.json
    fire-position-basic.json
    flow-field-open.field.json
```

When pathfinding changes intentionally, update golden files deliberately.

Golden files can store:

- selected cell
- path cell list
- path cost
- failure reason
- flow field summary
- candidate score ordering

---

## 15.9 Integration Tests

Integration tests verify that multiple modules work together.

Examples:

### Move Command Integration

```text
given a unit and a target point
when move command is issued
then a path or flow field is produced
and unit simulation makes progress
and unit eventually arrives or fails cleanly
```

### Nearest Free Cell Integration

```text
given target point inside blocked area
when move command is issued
then system selects nearest reachable free cell
and path ends at that cell
```

### Fire Position Integration

```text
given unit outside weapon range
when attack command is issued
then unit moves to valid firing cell
and firing condition becomes true
```

### Group Movement Integration

```text
given 20 units and one destination
when group move command is issued
then one shared flow field may be used
and units arrive near destination
and units do not permanently overlap
```

---

## 15.10 Scenario Tests

Scenario tests simulate gameplay-like situations.

Recommended scenarios:

1. Open field move with 50 units.
2. Move through narrow choke point.
3. Move to point inside partially blocked area.
4. Attack-move toward target point.
5. Ranged units seek firing position around obstacle.
6. Dynamic obstacle appears during movement.
7. Unit is given unreachable command.
8. Group destination has limited free cells.
9. Units retreat while enemies pursue.
10. Mixed melee and ranged units move together.

Success criteria:

- no crashes
- no infinite loops
- no permanent stuck units in normal scenarios
- acceptable movement behavior
- frame time remains within budget

---

## 15.11 Determinism Tests

Run the same command sequence multiple times:

```text
same map
same unit positions
same commands
same seed
```

Assert:

- path results match
- selected nearest cells match
- selected firing cells match
- flow field versions and costs match
- final unit positions are close enough if simulation has floating point variation

For stricter determinism:

- use fixed-point or integer simulation for pathfinding decisions
- avoid non-deterministic iteration order
- avoid hash-order dependence

---

## 15.12 Performance Tests

Performance tests should not always fail CI for small fluctuations, but they should be tracked.

Measure:

- A\* query time
- nodes expanded
- allocations per query
- nearest-free query time
- fire-position query time
- flow field generation time
- avoidance update time
- frame time with many units
- GC pressure

Example benchmark scenarios:

```text
100 individual A* queries
1000 nearest-free queries
100 fire-position queries
10 flow field generations
200 units avoidance update
500 units movement simulation
```

Use thresholds as warnings initially:

```text
A* small map: < 1 ms
flow field medium map: < 5 ms
avoidance 200 units: < 3 ms
```

Adjust thresholds based on actual target hardware.

---

## 15.13 Stress Tests

Stress tests check robustness under bad conditions.

Examples:

- 1000 path requests in one frame
- all units commanded to same tiny area
- path request queue overloaded
- many stale requests canceled
- dynamic obstacles toggled rapidly
- unit starts in blocked cell
- target moves every frame
- flow field destination becomes blocked

Expected behavior:

- no crashes
- no deadlocks
- no unbounded memory growth
- graceful degradation
- expired requests dropped

---

## 15.14 Concurrency Tests

Test asynchronous path requests.

Cases:

- request queue handles many requests
- cancellation works
- stale results are ignored
- results are applied only on update thread
- grid updates do not corrupt searches
- worker threads terminate cleanly

Use:

- thread sanitizers if available
- repeated random concurrent workloads
- timeouts to detect hangs

---

## 15.15 jMonkeyEngine Integration Tests

Core pathfinding should be tested without JME rendering.

However, integration tests should verify:

- world-to-grid conversion using JME coordinates
- terrain height sampling
- raycast/LOS adapter behavior
- debug drawing does not crash
- movement updates work in JME update loop
- commands from input produce expected path requests

If headless JME testing is awkward, use:

- manual test scenes
- recorded test scenarios
- offscreen screenshots where practical
- simple editor/debug app

---

## 15.16 Visual and Manual Tests

Some RTS behavior is hard to verify with assertions alone.

Manual test checklist:

- units respond quickly to move commands
- units do not jitter excessively
- groups do not explode apart
- units do not stack permanently
- choke points do not deadlock forever
- ranged units find reasonable firing positions
- nearest-free-cell movement looks natural
- debug overlays match actual behavior

Visual debug overlays:

- grid occupancy
- selected cells
- paths
- flow arrows
- range circles
- LOS lines
- avoidance vectors
- unit target cells

---

## 15.17 License Compliance Tests

Use dependency license reporting.

Checklist:

- generate license report in CI
- maintain allowed license list
- fail build on forbidden licenses unless explicitly approved
- keep third-party licenses in repository
- verify no GPL source files are vendored
- verify no copied GPL code in history or imports

Allowed license list:

```text
MIT
BSD-2-Clause
BSD-3-Clause
ISC
Zlib
Apache-2.0
```

Review list:

```text
LGPL
EPL
MPL
```

Forbidden without legal review:

```text
GPL-2.0
GPL-3.0
AGPL
```

For test-only dependencies, decide policy explicitly.

Example policy:

```text
Test-only EPL dependencies are acceptable because they are not distributed.
```

Or stricter:

```text
No EPL dependencies even in test scope.
```

---

## 15.18 Continuous Integration

CI should run:

```text
compile
unit tests
integration tests
license report
static analysis
architecture checks
quick performance smoke test
```

Scheduled nightly jobs can run:

```text
long performance benchmarks
stress tests
large-map scenario tests
random soak tests
```

---

## 15.19 Suggested Test Suites

### Fast Suite

Run on every commit:

```text
grid tests
A* tests
nearest-free tests
fire-position tests
flow-field invariant tests
basic integration tests
```

Target time:

```text
< 2 minutes
```

### Full Suite

Run on merge requests:

```text
fast suite
scenario tests
determinism tests
concurrency tests
medium performance tests
license report
```

Target time:

```text
< 10 minutes
```

### Nightly Suite

Run nightly:

```text
full suite
large benchmarks
stress tests
long soak tests
random map fuzzing
visual regression, if available
```

---

## 15.20 Example Test Cases

### TC-001: Simple A\*

Map:

```text
#####
#S.G#
#####
```

Expected:

- path length is direct
- path cells are free
- path ends at G

---

### TC-002: Obstacle Avoidance

Map:

```text
#####
#S#G#
#...#
#####
```

Expected:

- path goes around wall
- no blocked cells included

---

### TC-003: Unreachable Goal

Map:

```text
#####
#S#G#
#####
```

Expected:

- path query fails
- failure reason indicates unreachable or blocked

---

### TC-004: Nearest Free Cell

Map:

```text
######
#S.#G#
######
```

If G cell is blocked or target point is inside obstacle:

Expected:

- selected cell is nearest reachable free cell near G
- selected cell is not behind wall

---

### TC-005: Fire Position With LOS Block

Map:

```text
#######
#S...#
#.#..#
#.#T.#
#######
```

Expected:

- if LOS blocked, some candidates invalid
- system chooses cell with LOS if reachable
- fails if no LOS cell reachable

---

### TC-006: Group Flow Field

Map:

```text
########
#S....G#
#.####.#
#S....G#
########
```

Expected:

- units use flow field or group movement
- units reach destination area
- no permanent overlap

---

## 16. Definition of Done

The system can be considered ready for iteration review when:

- core pathfinding tests pass
- nearest-free-cell query works and is tested
- fire-position query works and is tested
- group movement works for target unit counts
- debug visualization is available
- no GPL code has been copied
- third-party licenses are documented
- performance is within agreed budgets
- known edge cases are documented or handled

---

## 17. Open Questions

These should be decided during implementation:

1. Grid cell size.
2. 4-direction vs 8-direction movement.
3. Are flying units supported?
4. Are buildings dynamic blockers?
5. Do units reserve destination cells?
6. Is formation movement required?
7. Is navmesh required, or is grid enough?
8. What is the target maximum unit count?
9. What is the target map size?
10. Is deterministic replay required?
11. Should pathfinding support multiple factions with different pathability rules?
12. Should terrain slope affect movement cost?

---

## 18. Summary

Recommended implementation:

```text
jMonkeyEngine for rendering/input
+
original permissively licensed RTS pathfinding module
+
grid-based pathability
+
A*/JPS for individuals and squads
+
flow fields for many units
+
tactical query layer for nearest free cell and firing positions
+
local avoidance
```

This approach avoids GPL reuse, supports RTS-scale movement, and provides a clear path for testing and iteration.