# Dune RTS — Java + JMonkeyEngine Implementation Plan

## 0. Key implementation principles

* Remember that you should follow SOLID principles, don't create any "God Classes"
* Add proper comments to your code
* Think about covering code with tests using JUnit/Mockito or specific approaches for JMonkey if there are any
* Add proper logging at all levels, this will simplify issue debug in future
* Always use buffered I/O (readers, streams) if you need to read any files in classical Java way

## 1. Technology Stack & Rationale

| Layer | Choice | Reason |
|---|---|---|
| **Language** | Java 17+ (LTS) | Records, sealed classes, pattern matching, modern GC |
| **3D Engine** | JMonkeyEngine 3.6+ | Scene graph, glTF/glb loader, spatial hierarchy, built-in particles, Nifty GUI |
| **Physics/Collision** | Custom (no Bullet) | Spec mandates simple radial/AABB collision without mesh physics |
| **Config Format** | TOML (toml4j) | Direct 1:1 mapping to spec's TOML block; human-editable |
| **Build** | Gradle (Kotlin DSL) | Modern build system, good JME plugin support |
| **Serialization** | JSON (Gson) + TOML | JSON for savegames, TOML for data definitions |
| **Logging** | SLF4J + Logback | Standard Java logging |
| **Entity Framework** | Custom ECS (Zay-ES inspired) | Lightweight, no reflection overhead, fast component queries |

---

## 2. Project Structure

```
dune-rts/
├── build.gradle.kts
├── settings.gradle.kts
├── assets/
│   ├── config/
│   │   ├── units/           # *.toml unit definitions
│   │   ├── weapons/         # *.toml weapon definitions
│   │   ├── particles/       # *.toml particle definitions
│   │   ├── locomotor/       # *.toml locomotor profiles
│   │   └── maps/            # *.json map definitions
│   ├── blender/             # Source .blend files (the "source of truth")
│   │   ├── units/           # e.g. heavy_tank.blend, rifleman.blend
│   │   └── buildings/       # e.g. barracks.blend, refinery.blend
│   ├── models/
│   │   ├── intermediate/    # *.glb exported from Blender (raw glTF)
│   │   ├── final/           # *.m3o (JME optimized binary; our importer output)
│   │   └── archives/        # *.tar.gz versioned model packs for distribution
│   ├── textures/
│   │   ├── atlases/         # PBR atlas textures (albedo, normal, roughness)
│   │   └── palette/         # shared tiling textures (terrain, decals)
│   │   └── fx/              # particle sprites (flash, smoke, spark, fire)
│   ├── sounds/
│   ├── fonts/
│   └── ui/
│       └── nifty/           # Nifty GUI XML layouts
├── src/
│   └── main/java/com/dunerpg/
│       ├── Main.java                    # Application entry, JME SimpleApplication
│       ├── core/
│       │   ├── ecs/
│       │   │   ├── Entity.java          # int id + component bitmask
│       │   │   ├── EntityManager.java   # create, destroy, query
│       │   │   ├── Component.java       # marker interface
│       │   │   └── ComponentType.java   # component enum for indexing
│       │   ├── systems/
│       │   │   ├── MovementSystem.java
│       │   │   ├── PathfindingSystem.java
│       │   │   ├── WeaponSystem.java
│       │   │   ├── TurretSystem.java
│       │   │   ├── CollisionSystem.java
│       │   │   ├── DockSystem.java
│       │   │   ├── ResourceGatherSystem.java
│       │   │   ├── ParticleSystem.java
│       │   │   ├── SelectionSystem.java
│       │   │   ├── AnimationSystem.java
│       │   │   └── DeathSystem.java
│       │   ├── components/
│       │   │   ├── Transform.java       # position, rotation, scale
│       │   │   ├── BodyComponent.java   # health, armor, maxHealth
│       │   │   ├── LocomotorComponent.java
│       │   │   ├── WeaponSetComponent.java
│       │   │   ├── TurretComponent.java
│       │   │   ├── CollisionShapeComponent.java
│       │   │   ├── DockComponent.java
│       │   │   ├── GatherComponent.java
│       │   │   ├── BuildComponent.java
│       │   │   ├── SelectableComponent.java
│       │   │   ├── ParticleEmitterComponent.java
│       │   │   ├── AnimationComponent.java
│       │   │   └── UnitFlagsComponent.java
│       │   └── state/
│       │       ├── UnitStateMachine.java
│       │       ├── WeaponStateMachine.java
│       │       ├── TurretState.java
│       │       ├── DockState.java
│       │       └── GatherState.java
│       ├── combat/
│       │   ├── WeaponTemplate.java
│       │   ├── Weapon.java
│       │   ├── WeaponSet.java
│       │   ├── DamageType.java
│       │   ├── DamageCalculator.java
│       │   ├── ArmorTable.java
│       │   └── CollideMask.java
│       ├── movement/
│       │   ├── Locomotor.java
│       │   ├── LocomotorTemplate.java
│       │   ├── LocomotorType.java
│       │   ├── Pathfinder.java          # A* grid-based
│       │   ├── PathRequest.java
│       │   ├── PathResult.java
│       │   └── GridMap.java
│       ├── collision/
│       │   ├── CollisionShape.java       # sealed interface
│       │   ├── RadialShape.java
│       │   ├── RectShape.java
│       │   ├── CollisionDetection.java   # SAT + circle math
│       │   └── CollisionEvent.java
│       ├── turret/
│       │   ├── TurretAI.java
│       │   ├── TurretTemplate.java
│       │   └── TurretStateMachine.java
│       ├── docking/
│       │   ├── DockProtocol.java
│       │   ├── DockType.java
│       │   └── DockAction.java
│       ├── resources/
│       │   ├── PlayerResources.java
│       │   ├── SupplyCycle.java
│       │   └── ResourceBox.java
│       ├── camera/
│       │   ├── RtsCamera.java           # JME ChaseCamera wrapper
│       │   ├── CameraBookmark.java
│       │   └── CameraShake.java
│       ├── input/
│       │   ├── RtsInputHandler.java     # JME InputManager integration
│       │   ├── SelectionBox.java
│       │   └── CommandDispatcher.java
│       ├── config/
│       │   ├── DataLoader.java          # TOML → Java object mapper
│       │   ├── UnitTemplate.java
│       │   ├── WeaponTemplateData.java
│       │   ├── ParticleTemplateData.java
│       │   └── MapData.java
│       ├── rendering/
│       │   ├── ModelLoader.java         # .m3o → JME Spatial + empties
│       │   ├── AnimationController.java # MODELCONDITION → bone anim
│       │   ├── ModelCondition.java      # enum matching spec
│       │   ├── MinimapRenderer.java
│       │   ├── FogOfWar.java
│       │   └── BillboardRenderer.java
│       ├── tools/
│       │   ├── importer/
│       │   │   ├── GltfImporter.java       # .glb → intermediate Node tree
│       │   │   ├── EmptyNodeResolver.java  # detects TurretPivot, BarrelPivot, etc.
│       │   │   ├── CollisionBaker.java     # auto-compute AABB/radius from mesh geometry
│       │   │   ├── M3oExporter.java        # serialize to .m3o custom binary format
│       │   │   └── BatchImporter.java      # walks assets/blender/, calls Blender CLI, imports all
│       │   └── preview/
│       │       ├── UnitPreviewApp.java     # standalone JME app for model preview
│       │       ├── TurretPreviewController.java  # rotate turret (yaw + pitch), fire particles
│       │       ├── ParticlePreview.java    # emit muzzle flash, smoke, engine exhaust
│       │       └── AnimationPreview.java   # cycle through all animations
│       ├── ui/
│       │   ├── HUDController.java
│       │   ├── CommandCard.java
│       │   ├── ProductionPalette.java
│       │   ├── ResourceBar.java
│       │   ├── MinimapWidget.java
│       │   ├── UnitInfoPanel.java
│       │   └── MainMenuScreen.java      # Nifty XML-backed
│       └── util/
│           ├── MathUtil.java
│           ├── BitMaskUtil.java
│           └── OctileDistance.java
```

---

## 3. Core Architecture: Entity-Component-System (ECS)

### 3.1 Entity

```java
public final class Entity {
    private final int id;
    private long componentMask;    // bitmask of attached component types
    private boolean alive;
}
```

### 3.2 ComponentType Enum (bit-indexed)

```java
public enum ComponentType {
    TRANSFORM(0),
    BODY(1),
    LOCOMOTOR(2),
    WEAPON_SET(3),
    TURRET(4),
    COLLISION_SHAPE(5),
    DOCK(6),
    GATHER(7),
    BUILD(8),
    SELECTABLE(9),
    PARTICLE_EMITTER(10),
    ANIMATION(11),
    UNIT_FLAGS(12),
    PROJECTILE(13);
    // ...
}
```

### 3.3 System Pipeline (per-frame order)

```
1. PathfindingSystem      — process A* requests, produce waypoint chains
2. MovementSystem         — locomotor acceleration/turn/position update
3. TurretSystem           — turret aiming, state machine transitions
4. WeaponSystem           — fire, reload, projectile spawn, hitscan
5. CollisionSystem        — radial/AABB collision detection + events
6. ProjectileSystem       — projectile movement + impact
7. DockSystem             — dock state machine (approach/enter/docked/exit)
8. ResourceGatherSystem   — harvester cycle (source → refinery)
9. DeathSystem            — death animation, destruction, cleanup
10. ParticleSystem        — emit, update, render (billboarded sprites)
11. AnimationSystem       — MODELCONDITION → bone animation graph
12. CameraSystem          — camera pan/zoom/rotate/shake update
13. FogOfWarSystem        — visibility calculations
14. SelectionSystem       — invalidate dead selections, update UI
```

---

## 4. Configuration Loading

### 4.1 TOML → Java Pipeline

```java
// DataLoader.java
public class DataLoader {
    public UnitTemplate loadUnitTemplate(String path);
    public WeaponTemplate loadWeaponTemplate(String path);
    public LocomotorTemplate loadLocomotorTemplate(String path);
    public ParticleTemplateData loadParticleTemplate(String path);
    public MapData loadMap(String path);
}
```

### 4.2 Boot Sequence

```
App start
 → Load all *.toml from assets/config/
 → Parse into template objects
 → Register each template in its Store (UnitStore, WeaponStore, LocomotorStore)
 → Show Main Menu
 → On "Start Game": load map, spawn initial entities from templates
```

---

## 5. Unit System Mapping

### 5.1 Spec → Java Classes

| Spec | Java Implementation |
|---|---|
| `ThingTemplate` | `config.UnitTemplate` (data class, TOML-loaded) |
| `Object` | `core.ecs.Entity` (int ID + components) |
| `BodyModule` | `BodyComponent` (health, armor, currentHealth) |
| `AIUpdateInterface` | `MovementSystem` + `WeaponSystem` (ECS systems, not per-entity) |
| `CollideModule` | `CollisionShapeComponent` + `CollisionSystem` |
| `Drawable` | JME `Spatial` (attached to entity via SpatialComponent or direct mapping) |
| `Physics (optional)` | Not used; death animation handled by `DeathSystem` with simple timer-based physics |

### 5.2 UnitFlag Translation

```java
// UnitFlagsComponent.java
public class UnitFlagsComponent implements Component {
    private int flags;  // bitmask matching spec's UnitFlags enum

    public boolean canMove()       { return (flags & 0x0001) != 0; }
    public boolean canBeSelected() { return (flags & 0x0002) != 0; }
    public boolean canBePicked()   { return (flags & 0x0004) != 0; }
    public boolean isStructure()   { return (flags & 0x0008) != 0; }
    public boolean isProjectile()  { return (flags & 0x0010) != 0; }
    public boolean isSelectable()  { return (flags & 0x0020) != 0; }
    public boolean isAirborne()    { return (flags & 0x0100) != 0; }
    public boolean isDockable()    { return (flags & 0x0200) != 0; }
    public boolean canBuild()      { return (flags & 0x0400) != 0; }
    public boolean canGather()     { return (flags & 0x0800) != 0; }
    public boolean hasTurret()     { return (flags & 0x1000) != 0; }
}
```

### 5.3 Entity ↔ Spatial Linking

JME's scene graph uses `Node` and `Spatial`. ECS entities reference a `Node`:

```java
public class SpatialComponent implements Component {
    Node node;           // JME scene graph node (Root of this unit's hierarchy)
    String modelPath;    // glb file loaded by JME AssetManager
}
```

The `Node`’s `UserData` maps JME lifecycle to entity ID, enabling click-picking:
```java
spatial.setUserData("entityId", entity.getId());
```

---

## 6. Locomotor System

### 6.1 Java Implementation

```java
public enum LocomotorType {
    LOCO_LEGS_TWO,     // infantry
    LOCO_WHEELS_FOUR,  // wheeled
    LOCO_TREADS,       // tracked
    LOCO_HOVER         // hover
}

public record LocomotorTemplate(
    LocomotorType type,
    float maxSpeed,
    float maxSpeedDamaged,
    float acceleration,
    float deceleration,
    float maxTurnRate,
    float minTurnSpeed,
    float closeEnoughDist,
    int surfaces,       // bitmask: ground=1, water=2
    float preferredHeight
) {}

public class LocomotorComponent implements Component {
    LocomotorTemplate template;
    // runtime state
    List<Vector3f> waypoints;  // from Pathfinder
    int currentWaypointIndex;
    Vector3f currentVelocity;
    float currentSpeed;
}
```

### 6.2 MovementSystem (per frame)

```java
// MovementSystem.java
public void update(float tpf) {
    for (Entity e : entitiesWith(LocomotorComponent.class, Transform.class)) {
        var loco = e.get(LocomotorComponent.class);
        var transform = e.get(Transform.class);
        var flags = e.get(UnitFlagsComponent.class);

        if (!flags.canMove() || loco.waypoints.isEmpty()) continue;

        Vector3f target = loco.waypoints.get(loco.currentWaypointIndex);
        Vector3f toTarget = target.subtract(transform.position);
        float dist = toTarget.length();

        if (dist < loco.template.closeEnoughDist()) {
            loco.currentWaypointIndex++;
            if (loco.currentWaypointIndex >= loco.waypoints.size()) {
                loco.waypoints.clear();
                // velocity → 0 via deceleration
            }
            continue;
        }

        // Accelerate toward desired velocity
        Vector3f desiredDir = toTarget.normalize();
        float desiredSpeed = Math.min(loco.template.maxSpeed, dist);

        // Apply acceleration/deceleration
        // Compute heading rotation (maxTurnRate clamped)
        // Apply to transform.position
        // Set MODELCONDITION_MOVING if speed > 0
    }
}
```

---

## 7. Pathfinding — Grid-Based A*

### 7.1 GridMap

```java
public class GridMap {
    int width, height;
    float cellSize;          // default: 10.0
    int[] cells;             // flat array, cell flags (CLEAR=0, BLOCKED=1, SOFT_BLOCKED=2)
    float[] terrainHeight;   // height at each cell center
}
```

### 7.2 Pathfinder

```java
public class Pathfinder {
    public PathResult findPath(Vector3f start, Vector3f goal, GridMap grid, LocomotorType loco);

    // A* with Octile distance heuristic
    // 8-directional movement (including diagonals)
    // Path optimization: remove colinear nodes
    // Soft-blocked cells: apply repulsion cost rather than rejection
    // Stuck detection: if blocked > 60 frames → re-path
}
```

### 7.3 Static Obstacle Baking

At map load time:
- Structures mark their footprint cells as `CELL_BLOCKED`
- Water cells (for non-hover units) marked as `CELL_BLOCKED`
- Dynamic units mark cells as `CELL_SOFT_BLOCKED` each frame (repulsion)

---

## 8. Collision System

### 8.1 Shape Hierarchy (Sealed Interface)

```java
public sealed interface CollisionShape
    permits RadialShape, RectShape {}

public record RadialShape(Vector3f center, float radius) implements CollisionShape {}
public record RectShape(Vector3f center, Vector3f halfExtents, float rotation) implements CollisionShape {}
```

### 8.2 CollisionDetection

```java
public class CollisionDetection {
    public static boolean test(RadialShape a, RadialShape b) {
        return a.center().distance(b.center()) < (a.radius() + b.radius());
    }

    public static boolean test(RectShape box, RadialShape circle) {
        // clamp circle center to box, check distance
    }

    public static boolean test(RectShape a, RectShape b) {
        // Separating Axis Theorem (SAT) — 4 axes, project both boxes
        // Ground collision: clamp Z to terrain height
    }
}
```

### 8.3 CollisionSystem (per frame)

```java
public class CollisionSystem {
    // Broad phase: grid-based spatial hash (cell size = ~20 units)
    // Narrow phase: CollisionDetection.test() per shape pair
    // Events dispatched: projectile impact, unit repulsion, dock entry
}
```

---

## 9. Terrain System

### 9.1 Core Principle

Terrain is a **height field** — a 2D array of floats. All game systems query it through the `TerrainHeightProvider` interface. The rendering mesh (quads → triangles) is a separate concern, generated deterministically from the same height data.

Three key separation rules:
- **No system** performs mesh-level collision against terrain
- **No system** reads heightmap PNGs directly at runtime
- **No system** depends on a concrete terrain class — only `TerrainHeightProvider`

### 9.2 TerrainHeightProvider Interface

```java
/**
 * Single abstraction for all terrain height queries.
 * Both flat (Stage 1) and heightmap-backed terrain implement this.
 * Thread-safe — designed for future worker-thread pathfinding.
 */
public interface TerrainHeightProvider {

    /**
     * @param worldX  world-space X coordinate
     * @param worldZ  world-space Z coordinate
     * @return terrain Y (height) at that point
     */
    float getHeight(float worldX, float worldZ);

    /** @return true if this point is water (height ≤ waterLevel) */
    boolean isWater(float worldX, float worldZ);

    /** @return gradient magnitude (0 = flat, higher = steeper) */
    float getGradient(float worldX, float worldZ);

    /** @return true if (x,z) is within map bounds */
    boolean isInBounds(float worldX, float worldZ);

    float getMapMinX();
    float getMapMaxX();
    float getMapMinZ();
    float getMapMaxZ();
    float getWaterLevel();
}
```

### 9.3 Implementations

#### 9.3.1 FlatTerrainHeightProvider (Stage 1 / Debug)

```java
/**
 * Returns constant height for all positions.
 * Used during Stage 1 development and for quick-test maps.
 */
public class FlatTerrainHeightProvider implements TerrainHeightProvider {

    private final float height;
    private final float mapSize;
    private final float waterLevel;

    @Override
    public float getHeight(float worldX, float worldZ) { return height; }

    @Override
    public boolean isWater(float worldX, float worldZ) { return false; } // never water on flat

    @Override
    public float getGradient(float worldX, float worldZ) { return 0f; }
}
```

#### 9.3.2 HeightmapTerrainProvider (Production)

```java
/**
 * Bilinear interpolation from a 16-bit PNG heightmap.
 *
 * Height field is stored as float[height][width] array (row-major).
 * getHeight(x,z) performs:
 *   1. World-to-grid: (col, row) = (x/cellSize, z/cellSize)
 *   2. Bilinear interpolation between 4 surrounding grid heights
 *   3. Clamp to map bounds (return 0 if out of bounds)
 */
public class HeightmapTerrainProvider implements TerrainHeightProvider {

    private final float[][] heights;       // [row][col]
    private final int width, height;       // grid dimensions (cells)
    private final float cellSize;          // world units per cell
    private final float waterLevel;        // threshold for isWater()
    private final float mapMinX, mapMaxX, mapMinZ, mapMaxZ;

    public HeightmapTerrainProvider(Path pngPath, float cellSize, float heightScale) {
        // 1. Load PNG → int[][] pixels (AWT BufferedImage)
        // 2. Normalize: heights[row][col] = (pixel / 65535f) * heightScale
        // 3. Store for getHeight()
        // 4. Precompute water cells for pathfinding grid
    }

    @Override
    public float getHeight(float worldX, float worldZ) {
        // World → grid coords
        float col = (worldX - mapMinX) / cellSize;
        float row = (worldZ - mapMinZ) / cellSize;

        // Clamp to edges
        int col0 = (int) Math.floor(col);
        int row0 = (int) Math.floor(row);
        int col1 = Math.min(col0 + 1, width - 1);
        int row1 = Math.min(row0 + 1, height - 1);

        // Bilinear interpolation weights
        float fx = col - col0;
        float fy = row - row0;

        float h00 = heights[row0][col0];
        float h10 = heights[row0][col1];
        float h01 = heights[row1][col0];
        float h11 = heights[row1][col1];

        return lerp(lerp(h00, h10, fx), lerp(h01, h11, fx), fy);
    }

    /** Central-difference gradient for slope checks */
    @Override
    public float getGradient(float wx, float wz) {
        float cx = getHeight(wx, wz);
        float dx = getHeight(wx + cellSize * 0.5f, wz);
        float dz = getHeight(wx, wz + cellSize * 0.5f);
        return (float) Math.sqrt(
            (dx - cx) * (dx - cx) + (dz - cx) * (dz - cx)
        ) / (cellSize * 0.5f);
    }
}
```

### 9.4 Mesh Generation (TerrainMeshGenerator)

```java
/**
 * Builds a JME Mesh from the height field.
 *
 * Output: regular grid of quads, each split into 2 triangles.
 *   (width × height quads = 2 × width × height triangles)
 *
 * Vertex format:
 *   Position: (x * cellSize, heights[row][col], z * cellSize)
 *   Normal:   central-difference from neighbors
 *   UV:       (col / width, row / height) for texture tiling
 *   Tangent:  for normal-mapped terrain (future)
 *
 * The mesh is generated once at map load and kept static
 * (no runtime modification unless using vertex shader morph).
 */
public class TerrainMeshGenerator {

    /**
     * @param heightProvider source of height data
     * @param mapMinX, mapMaxX, mapMinZ, mapMaxZ bounds
     * @param cellSize world units per grid cell
     * @param texScale uv repeat scale for tiling texture
     * @return JME Mesh ready for Geometry
     */
    public static Mesh generateMesh(
        TerrainHeightProvider heightProvider,
        float mapMinX, float mapMaxX, float mapMinZ, float mapMaxZ,
        float cellSize, float texScale
    ) {
        int cols = (int) ((mapMaxX - mapMinX) / cellSize) + 1;
        int rows = (int) ((mapMaxZ - mapMinZ) / cellSize) + 1;

        // 1. Allocate buffers: position[cols*rows*3], normal[...], uv[...], index[cols*rows*6]
        // 2. For each (col, row):
        //      wx = mapMinX + col * cellSize
        //      wz = mapMinZ + row * cellSize
        //      y = heightProvider.getHeight(wx, wz)
        //      Set position, uv
        // 3. Compute normals via cross product of adjacent edge vectors
        // 4. Build index buffer: two triangles per quad
        // 5. Return Mesh with Type.Triangle
    }

    /**
     * Variant that splits each quad into 4 sub-quads (for higher visual resolution
     * without increasing heightmap resolution).
     */
    public static Mesh generateTessellatedMesh(/* ... */) { /* ... */ }
}
```

### 9.5 Water Surface

```java
/**
 * Single flat mesh rendered at waterLevel height.
 * Semi-transparent blue material with animated UV offset for wave effect.
 * Placed in a separate Geometry above the terrain.
 */
public class WaterSurface {

    private final Geometry waterGeometry;
    private float uvOffset;   // animated each frame

    public WaterSurface(TerrainHeightProvider provider, AssetManager assetManager) {
        // Create flat quad(s) covering all cells where isWater() == true
        // Or simpler: single large quad at waterLevel for the whole map
        // With pixel shader discarding above-water fragments (alpha clip)
    }

    public void update(float tpf) {
        // Scroll UV for wave animation
        uvOffset += tpf * 0.1f;
        waterGeometry.getMaterial().setTextureOffset("DiffuseMap",
            new Vector2f(uvOffset, uvOffset));
    }
}
```

### 9.6 Pathfinding Grid Integration

At map load, the pathfinding GridMap's cell flags are baked from terrain + static objects:

```java
GridMap grid = new GridMap(mapWidth, mapHeight, cellSize);

// Step 1: Mark water cells
for each cell (col, row):
    float cx = grid.cellCenterX(col);
    float cz = grid.cellCenterZ(row);
    if (terrainProvider.isWater(cx, cz)) {
        grid.setCell(col, row, CELL_BLOCKED);  // for non-hover
        // hover units: CELL_CLEAR (checked at locomotor level)
    }

// Step 2: Mark blocked for steep gradient
for each cell:
    float gradient = terrainProvider.getGradient(grid.cellCenterX(col), grid.cellCenterZ(row));
    if (gradient > MAX_WALKABLE_GRADIENT) {
        grid.setCell(col, row, CELL_BLOCKED);
    }

// Step 3: Structure footprints (added at build time)
// In canPlaceBuilding(): mark cells as CELL_BLOCKED in the grid
```

### 9.7 Object Placement Rules

```java
/**
 * Validates and executes placement of buildings, trees, and decorations.
 */
public class PlacementValidator {

    private final TerrainHeightProvider terrain;
    private final GridMap pathfindingGrid;
    private final float maxBuildSlope;  // max height difference across a building's footprint

    /**
     * Check if a building can be placed.
     * @param footprintWidth, footprintDepth in world units
     * @return details about placement feasibility
     */
    public PlacementResult canPlace(
        float worldX, float worldZ, float rotation,
        float footprintWidth, float footprintDepth
    ) {
        // 1. Get AABB corners from position + footprint + rotation
        // 2. For each pathfinding cell overlapping footprint:
        //      if cell is CELL_BLOCKED → FAIL (BLOCKED)
        //      if terrain.isWater(...) → FAIL (WATER) for non-hover buildings
        // 3. Sample height at 4 corners:
        //      h1 = terrain.getHeight(nearX, nearZ), etc.
        // 4. Compute slope: max(|h1-h2|, |h1-h3|, |h1-h4|, ...)
        //      if slope > maxBuildSlope → FAIL (TOO_STEEP)
        // 5. Place building Y = average of 4 corner heights
        // 6. Mark footprint cells as CELL_BLOCKED
    }

    /**
     * Tree/decorator placement for map authoring.
     * No collision involvement — purely visual.
     */
    public boolean canPlaceDecor(
        float worldX, float worldZ, float minSpacing
    ) {
        // Check not in water, not too steep, minimum distance from other decor
    }
}
```

### 9.8 TerrainConfig (TOML-based Map Definition)

```toml
[map]
name = "Desert Hills"
width = 512          # world units
depth = 512
cell_size = 1.0      # world units per heightmap cell

[heightmap]
image = "maps/desert_hills_hm.png"
height_scale = 8.0   # maps 0..65535 → 0..8 world units
water_level = 0.2    # cells below this height are water

[lighting]
sun_angle = [45, 30]  # azimuth, elevation in degrees
sun_color = [1.0, 0.95, 0.85]
ambient_color = [0.3, 0.3, 0.35]

[objects]
# Neutral/civilian buildings
[[buildings]]
template = "civilian_hut"
position = [120, 0, 80]
rotation = 0.0

# Trees (auto-scatter density)
[[vegetation]]
variant = "palm"
scatter_density = 0.02    # trees per world unit²
seed = 42
```

### 9.9 Integration with Collision System

The collision system (Section 8) handles **unit↔unit** and **projectile↔unit** collisions only. Terrain collision is replaced by ground clamping:

```java
// In MovementController or per-frame update:
Vector3f pos = unit.position();
pos.y = terrainProvider.getHeight(pos.x, pos.z) + unit.getPreferredHeight();
unit.spatial().setLocalTranslation(pos);
```

The `TurretController` and `MousePicker.pickTerrain` both query `terrainProvider.getHeight()` directly instead of raycasting against the terrain mesh. This is faster, simpler, and avoids mesh-collision entirely.

---

## 10. Weapon System

### 9.1 WeaponTemplate

```java
public record WeaponTemplate(
    String name,
    float primaryDamage,
    float primaryDamageRadius,
    float secondaryDamage,
    float secondaryDamageRadius,
    float attackRange,
    float minimumAttackRange,
    float weaponSpeed,           // 0 = instant/hitscan
    DamageType damageType,
    DeathType deathType,
    int clipSize,                // 0 = infinite
    int clipReloadTime,
    int delayBetweenShots,
    int shotsPerBarrel,
    int preAttackDelay,
    ReloadType reloadType,
    PrefireType prefireType,
    int collideMask,
    String projectileTemplate,   // null for hitscan
    String fireFX,
    String detonateFX
) {}
```

### 9.2 WeaponInstance (Runtime)

```java
public class Weapon {
    WeaponTemplate template;
    WeaponState state;            // READY, PRE_ATTACK, FIRING, BETWEEN_SHOTS, OUT_OF_AMMO, RELOADING
    int ammoInClip;
    int shotTimer;
    Entity currentTarget;
}
```

### 9.3 WeaponStateMachine

```
READY_TO_FIRE → target in range → PRE_ATTACK (preAttackDelay ticks)
  → FIRE (spawn projectile or hitscan apply)
  → BETWEEN_SHOTS (delayBetweenShots ticks) → READY_TO_FIRE or OUT_OF_AMMO
OUT_OF_AMMO → RELOADING (clipReloadTime) → READY_TO_FIRE
```

### 9.4 WeaponSet Selection

```java
public Weapon chooseBestWeaponForTarget(Entity source, Entity target) {
    // 1. Filter weapons in range & not too close
    // 2. Filter by anti-mask (can this weapon hit that unit type?)
    // 3. Prefer highest estimatedDamage
    // 4. Tie-break: PRIMARY > SECONDARY > TERTIARY
}
```

### 9.5 WeaponSystem (per frame)

```java
public class WeaponSystem {
    // Iterate entities with WeaponSetComponent
    //   → For each weapon slot on auto-fire:
    //       → Scan for targets within guard range
    //       → Choose best weapon via chooseBestWeaponForTarget
    //       → Advance weapon state machine
    //   → For hitscan: apply damage immediately
    //   → For projectile: spawn ProjectileEntity
}
```

---

## 11. Turret System

### 11.1 Turret Implementation via JME Bone Manipulation

### 11.2 Turret State Machine (Java Enum)

### 11.3 TurretSystem (per frame)

```java
public class TurretSystem {
    public void update(float tpf) {
        for (Entity e : entitiesWith(TurretComponent.class)) {
            var turret = e.get(TurretComponent.class);

            switch (turret.state) {
                case AIM:
                    // Compute angle to target via ThePartitionManager::getRelativeAngle2D equivalent
                    // Rotate turretPivotNode Y-axis at turnRate toward target
                    // If allowsPitch: rotate barrelPivotNode X-axis toward target pitch
                    // If sweep enabled: add ±sweepAngle wobble
                    // Set MODELCONDITION_TURRET_ROTATE while turning
                    // If aligned: → FIRE
                    break;
                case IDLE_SCAN:
                    // Rotate to random angle within [minIdleScanAngle, maxIdleScanAngle]
                    break;
                case RECENTER:
                    // Rotate back to naturalTurretAngle at half turnRate
                    break;
                // ...
            }
        }
    }
}
```

---

## 12. MODELCONDITION Animation System

### 12.1 ModelCondition Enum
### 12.2 AnimationController
### 12.3 AnimationSystem

```java
public class AnimationSystem {
    // Each frame:
    //   Read active ModelConditions from entity
    //   Find highest-priority condition with matching animation
    //   Crossfade/blend to that animation via AnimControl
    //   Priority order: DYING > FIRING > PREATTACK > MOVING > DEPLOYED > ... > IDLE
}
```

---

## 13. Docking & Deployment

### 13.1 DockState Enum
### 13.2 DockComponent
### 13.3 DockAction (Sealed Interface)
### 13.4 Deploy/Undeploy

```java
public class BuildComponent implements Component {
    boolean deployed;
    float deployTimer;
    BuilderEntity builder;   // worker/dozer entity
    float buildProgress;     // 0..1
}
```

---

## 14. Resource Gathering

### 14.1 GatherState Enum
### 14.2 PlayerResources
### 14.3 ResourceGatherSystem

```java
public class ResourceGatherSystem {
    // For each entity with GatherComponent:
    //   State machine transitions based on position proximity
    //   Warehouse: findBestWarehouse() within scan distance
    //   At source: gainBoxes() up to maxBoxes
    //   At refinery: loseBoxes() → player.deposit(boxes * boxValue)
    //   Auto-repeat if configured
}
```

---

## 15. Camera System

### 15.1 RtsCamera
### 15.2 Camera Update (per frame)
### 15.3 Input Handling (via JME InputManager)
### 15.4 Screen↔World Mapping
### 15.5 CameraShake

```java
public class CameraShake {
    Vector3f epicenter;
    float intensity;
    float duration;
    float elapsed;

    void apply(RtsCamera cam) {
        float factor = 1.0f - (elapsed / duration); // decay
        float distFactor = 1.0f / (1.0f + pivot.distance(epicenter));
        cam.offset = randomVec3f() * intensity * factor * distFactor;
    }
}
```

---

## 16. Selection & Input System

### 16.1 SelectionBox (JME Screen-Based)
### 16.2 Selection Rules Logic
### 16.3 CommandDispatcher
### 16.4 Control Groups

```java
// Per-player
Map<Integer, Set<Entity>> controlGroups = new HashMap<>();  // 1..9

// Ctrl+number → store selected entities
// number → recall/select control group
// Double-number → recall + center camera on group
```

---

## 17. Particle System

### 16.1 JME Integration

JME has a built-in `ParticleEmitter` (`com.jme3.effect.ParticleEmitter`). We extend it with the spec's parameters:

```java
public class ParticleSystem {
    // wraps ParticleEmitter per entity
    // Each emitter configured from ParticleTemplateData (TOML)

    void emit(ParticleTemplateData template, Vector3f position, Vector3f velocity);
    void update(float tpf);   // age particles, apply acceleration/velocity/lerp
    void render();             // delegate to JME's ParticleEmitter rendering

    // Billboarding: JME ParticleEmitter handles this natively
    // Blend modes: ADDITIVE, ALPHA, MULTIPLY mapped to JME Material BlendMode
    // Wind: apply global wind vector from map settings
}
```

### 16.2 Effect Triggers Mapping

| Game Event | Particle Effect | Sound |
|---|---|---|
| Weapon fires | Gun Flash (muzzle bone) + Muzzle Smoke | fireSound |
| Projectile impact | Explosion/Spark (contact point) + camera shake | detonationSound |
| Unit dies | Death Smoke + optional Explosion | deathSound |
| Structure damaged | Building Dust (continuous) | — |
| Projectile in flight | Trail/Exhaust (continuous) | — |
| Unit moves | Track dust / engine smoke | — |

### 16.3 Particle System → JME Render Queue

Particle emitters need a special render bucket to render as billboarded sprites with correct depth sorting. JME's `ParticleEmitter` already handles this; we use the `Bucket.Transparent` with proper depth write settings.

---

## 18. Blender Asset Import Pipeline

### 17.1 Pipeline Overview

```
                    ┌─────────────────────┐
                    │  Blender (.blend)   │  Artist works here
                    │  Source of truth    │
                    └─────────┬───────────┘
                              │ Export (glTF 2.0, .glb, binary)
                              │ via Blender CLI or GUI
                              ▼
                    ┌─────────────────────┐
                    │  Intermediate .glb  │  Raw glTF with empties,
                    │  assets/models/     │  meshes, armatures
                    │  intermediate/      │
                    └─────────┬───────────┘
                              │ Our Custom Importer (GltfImporter.java)
                              │ Reads .glb via JME GltfLoader
                              │ Resolves named empties
                              │ Auto-bakes collision (AABB / radius)
                              │ Extracts bone/bone-attach-point metadata
                              │ Validates model contract
                              ▼
                    ┌─────────────────────┐
                    │  Final .m3o         │  JME-optimized binary
                    │  assets/models/     │  Includes: meshes, materials,
                    │  final/             │  empty-node manifest,
                    │                     │  precomputed collision data,
                    │                     │  animation clips index
                    └─────────┬───────────┘
                              │ Runtime load via ModelLoader.java
                              ▼
                    ┌─────────────────────┐
                    │  JME Spatial (Node) │  Ready to use in-game
                    │  + EntityMetadata   │
                    └─────────────────────┘
```

### 17.2 Blender Export Conventions (Artist Contract)

Artists work in `assets/blender/`. Each `.blend` must conform to the naming contract from the original spec (Section 14).

**Blender → glTF export settings (automated by BatchImporter):**

```
Format:        glTF Binary (.glb)
Meshes:        Apply modifiers (✓), Triangulate faces (✓)
Transform:     +Y Up (matches JME coordinate system)
Animations:    All actions, sampled keyframes, always sample (✓)
Compression:   Draco mesh compression (optional, configurable)
Scale:         1 Blender unit = 1 JME world unit
Empty Objects: Exported as glTF Nodes (NO mesh, NO skin, just transform)
```

**Blender CLI invocation** (headless export by `BatchImporter.java`):

```
blender --background heavy_tank.blend --python-expr "
import bpy
bpy.ops.export_scene.gltf(
    filepath='.../models/intermediate/heavy_tank.glb',
    export_format='GLB',
    export_yup=True,
    export_apply=True,
    export_animations=True,
    export_empties=True
)"
```

### 17.3 Custom Importer — GltfImporter.java

```java
/**
 * Reads a .glb file (intermediate) and produces:
 *   1. A JME Spatial Node tree (meshes, materials, bones)
 *   2. A ModelManifest describing empties, collision bounds, animations
 *
 * This runs as a build-time tool, NOT at game runtime.
 * The output is serialized to .m3o for runtime loading.
 */
public class GltfImporter {

    public ImportResult importGlb(Path glbPath, AssetManager assetManager) {
        // 1. Load raw .glb via JME's GltfLoader
        Node rawNode = (Node) assetManager.loadModel(glbPath.toString());

        // 2. Walk the node tree and classify each node
        ModelManifest manifest = analyzeHierarchy(rawNode);

        // 3. Auto-bake collision bounds from mesh geometry
        bakeCollisionShapes(rawNode, manifest);

        // 4. Validate completeness (mandatory nodes present, no orphans)
        validateContract(manifest);

        // 5. Extract animation clip definitions
        extractAnimationClips(rawNode, manifest);

        return new ImportResult(rawNode, manifest);
    }
}
```

### 17.4 Empty Node Resolution & Classification (EmptyNodeResolver.java)

```java
/**
 * Recursively walks the glTF node hierarchy and classifies
 * each Node by matching its name (case-insensitive) against
 * the known naming convention.
 *
 * Returns a ModelManifest that maps every known empty to its
 * Node reference and local transform.
 */
public class EmptyNodeResolver {

    // Regex patterns for classification (case-insensitive matching)
    private static final Map<Pattern, NodeRole> ROLE_PATTERNS = Map.of(
        Pattern.compile("(?i).*turretpivot.*"),    NodeRole.TURRET_PIVOT,
        Pattern.compile("(?i).*barrelpivot.*"),    NodeRole.BARREL_PIVOT,
        Pattern.compile("(?i).*muzzle.*"),          NodeRole.MUZZLE,
        Pattern.compile("(?i).*dockingpoint.*"),    NodeRole.DOCKING_POINT,
        Pattern.compile("(?i).*track(s)?.*"),       NodeRole.TRACKS,
        Pattern.compile("(?i).*smoke.*"),           NodeRole.SMOKE,
        Pattern.compile("(?i).*spawnpoint.*"),      NodeRole.SPAWN_POINT,
        Pattern.compile("(?i).*exitpoint.*"),       NodeRole.EXIT_POINT,
        Pattern.compile("(?i).*dockpoint.*"),       NodeRole.DOCK_POINT,
        Pattern.compile("(?i).*wheel_fl.*"),        NodeRole.WHEEL_FL,
        Pattern.compile("(?i).*wheel_fr.*"),        NodeRole.WHEEL_FR,
        Pattern.compile("(?i).*wheel_rl.*"),        NodeRole.WHEEL_RL,
        Pattern.compile("(?i).*wheel_rr.*"),        NodeRole.WHEEL_RR,
        Pattern.compile("(?i).*chassis.*"),         NodeRole.CHASSIS,
        Pattern.compile("(?i).*turret(?!.*pivot).*"), NodeRole.TURRET_MESH,
        Pattern.compile("(?i).*barrel(?!.*pivot).*"), NodeRole.BARREL_MESH,
        Pattern.compile("(?i).*ramp.*"),            NodeRole.RAMP
    );

    public static ModelManifest resolve(Node rootNode) {
        ModelManifest manifest = new ModelManifest();
        resolveRecursive(rootNode, manifest);
        return manifest;
    }

    private static void resolveRecursive(Node node, ModelManifest manifest) {
        String name = node.getName() != null ? node.getName() : "";

        for (var entry : ROLE_PATTERNS.entrySet()) {
            if (entry.getKey().matcher(name).matches()) {
                manifest.register(entry.getValue(), node);
                break;
            }
        }

        for (Spatial child : node.getChildren()) {
            if (child instanceof Node childNode) {
                resolveRecursive(childNode, manifest);
            }
        }
    }
}

public enum NodeRole {
    TURRET_PIVOT,      // yaw rotation center (local Y axis)
    BARREL_PIVOT,      // pitch rotation center (local X axis)
    MUZZLE,            // projectile spawn point + flash FX origin
    DOCKING_POINT,     // where harvesters attach during docking
    TRACKS,            // road dust particle emission point
    SMOKE,             // engine exhaust particle emission point
    SPAWN_POINT,       // new units appear here (buildings)
    EXIT_POINT,        // units drive/walk out from here (buildings)
    DOCK_POINT,        // external dock attachment point (buildings)
    WHEEL_FL, WHEEL_FR, WHEEL_RL, WHEEL_RR,  // individual wheels
    CHASSIS,           // main body mesh (collision source)
    TURRET_MESH,       // visual turret model
    BARREL_MESH,       // visual barrel model
    RAMP               // vehicle deployment ramp (buildings)
}
```

### 17.5 Auto-Baking Collision Shapes (CollisionBaker.java)

```java
/**
 * Computes collision bounds directly from mesh vertex data.
 * No manual authoring required — the artist drops a "Chassis" mesh
 * and gets working collision automatically.
 */
public class CollisionBaker {

    /**
     * Computes AABB (center + half-extents) from all Geometry children
     * under the Chassis node. The AABB is axis-aligned in model-local space
     * and will be transformed to world-space by the entity's Transform at runtime.
     */
    public static RectShape bakeAABB(Node chassisNode) {
        Vector3f min = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
        Vector3f max = new Vector3f(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);

        // Walk entire sub-tree under Chassis, accumulate vertex bounds
        chassisNode.depthFirstTraversal(spatial -> {
            if (spatial instanceof Geometry geom) {
                Mesh mesh = geom.getMesh();
                FloatBuffer posBuf = (FloatBuffer) mesh.getBuffer(Type.Position).getData();
                posBuf.rewind();
                while (posBuf.hasRemaining()) {
                    float x = posBuf.get(), y = posBuf.get(), z = posBuf.get();
                    // Transform vertex by geometry's world matrix (local → model space)
                    Vector3f local = geom.getWorldMatrix().mult(new Vector3f(x, y, z));
                    min.minLocal(local);
                    max.maxLocal(local);
                }
            }
        });

        Vector3f center = min.add(max).multLocal(0.5f);
        Vector3f halfExtents = max.subtract(min).multLocal(0.5f);
        return new RectShape(center, halfExtents, 0f);
    }

    /**
     * Computes bounding-sphere radius for a RadialShape from AABB.
     * sphere.radius = max(halfExtents.x, halfExtents.y, halfExtents.z)
     * Used for infantry, projectiles, small vehicles.
     */
    public static RadialShape bakeRadius(RectShape aabb) {
        float radius = Math.max(
            Math.max(aabb.halfExtents().x, aabb.halfExtents().y),
            aabb.halfExtents().z
        );
        return new RadialShape(aabb.center(), radius);
    }

    /**
     * Determines which shape type to use based on unit category.
     *   Infantry, projectiles → RadialShape (bounding sphere)
     *   Tanks, buildings, structures → RectShape (AABB)
     */
    public static CollisionShape autoShape(String category, Node chassisNode) {
        RectShape aabb = bakeAABB(chassisNode);
        if (category.equals("Soldier") || isProjectile) {
            return bakeRadius(aabb);
        }
        return aabb;
    }
}
```

### 17.6 ModelManifest & .m3o Serialization (M3oExporter.java)

```java
/**
 * Data structure captured during import and serialized
 * into the .m3o file alongside JME's native mesh data.
 */
public class ModelManifest {
    String modelName;
    String category;            // Soldier, AFV, Structure, etc.

    // Empty-node-to-role mapping (only for detected nodes)
    Map<NodeRole, NodeRef> roles = new EnumMap<>(NodeRole.class);

    // Pre-baked collision data
    CollisionShape collisionShape;    // radial or AABB
    float preferredHeight;            // hover / ground offset

    // Animation clips
    Map<String, AnimationClip> animationClips;  // walk, idle, fire, death, etc.

    // Bone-attach points (for particle emitters at runtime)
    List<BoneSlot> boneSlots = new ArrayList<>();

    /** A bone-attach point for runtime FX. */
    public record BoneSlot(String boneName, BoneSlotType type) {}
    public enum BoneSlotType { MUZZLE_FLASH, ENGINE_SMOKE, TRACK_DUST, DEATH_SMOKE }
}

/**
 * .m3o format: custom binary file = JME's native save(j3o) + our manifest.
 *
 * Layout:
 *   [4 bytes]  Magic: 0x4D334F00  ("M3O\0")
 *   [4 bytes]  Version
 *   [4 bytes]  Manifest JSON length (LE)
 *   [N bytes]  Manifest JSON (Gson-serialized ModelManifest)
 *   [M bytes]  JME BinaryImporter data (Spatial + meshes + materials + armature)
 *
 * This preserves JME's native material/mesh/animation serialization
 * while adding our structured metadata as a JSON header.
 */
public class M3oExporter {

    public static void exportM3o(ImportResult result, Path outputPath) {
        // 1. Serialize manifest to Gson JSON bytes
        byte[] manifestBytes = GSON.toJson(result.manifest()).getBytes(StandardCharsets.UTF_8);

        // 2. Serialize JME spatial via BinaryExporter to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BinaryExporter.getInstance().save(result.node(), baos);
        byte[] jmeBytes = baos.toByteArray();

        // 3. Write .m3o
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(outputPath.toFile()))) {
            out.writeInt(0x4D334F00);  // magic
            out.writeInt(1);           // version
            out.writeInt(manifestBytes.length);
            out.write(manifestBytes);
            out.write(jmeBytes);
        }
    }
}
```

### 17.7 Runtime ModelLoader.java (reads .m3o)

```java
public class ModelLoader {

    public LoadedModel loadM3o(String path, AssetManager assetManager) {
        // 1. Read .m3o header: magic, version, manifest JSON
        // 2. Deserialize manifest via Gson → ModelManifest
        // 3. Delegate to JME BinaryImporter for the spatial payload
        // 4. Re-link empty nodes from manifest to the spatial tree
        //    (manifest stores node paths; walk tree to find them)
        // 5. Return LoadedModel(spatial, manifest)
        // 6. Cache in assetManager (same model loaded once)
    }
}

public record LoadedModel(
    Node spatial,            // Full JME scene-graph tree
    ModelManifest manifest   // Structured metadata
) {}

```

### 17.8 BatchImporter — CLI Tool for Build Pipeline

```java
/**
 * Standalone Java CLI tool invoked during build:
 *   java -cp dune-tools.jar com.jrts.tools.importer.BatchImporter
 *
 * Workflow:
 *   1. Walk assets/blender/ → find all .blend files
 *   2. For each .blend: invoke Blender CLI → export .glb to models/intermediate/
 *   3. For each .glb: invoke GltfImporter → produce ImportResult
 *   4. Validate ModelManifest against contract
 *   5. Export .m3o to models/final/
 *   6. Optionally package .m3o files into models/archives/modelpack_v1.tar.gz
 *
 * Incremental mode: skip .glb export if .blend unchanged since last run
 *   (hash .blend file, compare to cached hash in models/intermediate/.hashes)
 */
public class BatchImporter {
    public static void main(String[] args) {
        // 1. Discover all .blend files
        // 2. Export to .glb via Blender --background
        // 3. Import .glb → bake collision, resolve empties
        // 4. Validate, warn on missing mandatory nodes
        // 5. Export to .m3o
        // 6. Print summary report
    }
}
```

### 17.9 Import Contract Validation

For each model category, certain empty nodes are mandatory. Missing mandatory nodes produce a **build error**; missing optional nodes produce a **warning**.

| Category | Mandatory Nodes | Optional Nodes |
|---|---|---|
| Vehicle (AFV, IFV) | `Chassis`, `TurretPivot` | `BarrelPivot`, `Muzzle`, `Tracks`, `Smoke`, `Wheel_*` |
| Infantry (Soldier) | `Chassis` | `Muzzle` (weapon bone) |
| Building (Structure) | `Base` | `TurretPivot`, `SpawnPoint`, `ExitPoint`, `DockPoint`, `Smoke`, `Ramp` |
| Projectile | `Chassis` | (none) |

```java
public List<String> validateContract(ModelManifest manifest, String category) {
    List<String> errors = new ArrayList<>();
    Set<NodeRole> mandatory = MANDATORY_BY_CATEGORY.get(category);
    for (NodeRole role : mandatory) {
        if (!manifest.roles().containsKey(role)) {
            errors.add("MISSING " + role + " in " + manifest.modelName());
        }
    }
    return errors;
}
```

### 17.10 Texture Atlas Resolution

During import, the importer detects texture references in the glTF material slots and resolves them against the convention:

```
Naming convention: {unitname}_atlas.png
Location: assets/textures/atlases/{unitname}_atlas.png

Resolution order:
  1. Check glTF material's texture URI directly (absolute/relative)
  2. If missing, derive from model name: "{modelName}_atlas.png"
  3. If still not found → fallback to solid color material → WARNING

Future PBR support:
  - {unitname}_atlas.png        → albedo (required)
  - {unitname}_atlas_normal.png → normal map (optional, adds bump)
  - {unitname}_atlas_glossy.png → metallic + roughness packed (optional)
```

---

## 19. Unit Preview App (Java + JME Standalone)

The Unit Preview App is a standalone JME desktop application (`UnitPreviewApp.java`) that lets artists and developers inspect imported models interactively **without launching the full game**. It boots a simple JME scene with lighting and orbit camera, loads a `.m3o` model, and provides controls to exercise all dynamic features.

### 18.1 Launch Modes

```
java -cp dune-tools.jar com.jrts.tools.preview.UnitPreviewApp [path]

  path = path to .m3o file   → preview single model
  path = path to directory    → file-browser mode, list all .m3o
  (no args)                   → file-picker dialog via JFileChooser
```

### 18.2 UI Layout

```
+----------------------------------------------------------+
|  Unit Preview: heavy_tank.m3o                    [X]     |
+----------------------------------------------------------+
|                           |                               |
|                           |  Camera Controls              |
|     3D Preview Viewport   |  [Reset] [Top] [Front] [Side]|
|     (turret rotates,      |                               |
|      particles emit,      |  Turret Controls              |
|      anims cycle)         |  Yaw:   [<] [-45°] [+45°] [>]|
|                           |  Pitch: [<] [-10°] [+45°] [>]|
|                           |  [Auto Idle Scan]             |
|                           |                               |
|                           |  FX Emitters                  |
|                           |  [Fire Muzzle Flash]          |
|                           |  [Toggle Engine Smoke]        |
|                           |  [Toggle Track Dust]          |
|                           |                               |
|                           |  Animations                   |
|                           |  [IDLE] [WALK] [FIRE] [DEATH] |
|                           |  [DEPLOY] [DOCK]              |
|                           |  [Stop All]                   |
|                           |                               |
|                           |  Collision Viz                |
|                           |  [✓] Show AABB / Radius       |
|                           |                               |
|                           |  Model Info                   |
|                           |  Verts: 4,200    Tris: 2,800  |
|                           |  Bones: 24       Anims: 5     |
|                           |  AABB: 3.2×4.8×2.1            |
|                           |  Radius: 3.0                  |
+---------------------------+-------------------------------+
```

### 18.3 PreviewScene Setup

```java
public class UnitPreviewApp extends SimpleApplication {

    private Node modelRoot;
    private ModelManifest manifest;

    // Rotatable references resolved from manifest
    private Node turretPivotNode;
    private Node barrelPivotNode;

    // Particle emitters
    private ParticleEmitter muzzleFlashEmitter;
    private ParticleEmitter engineSmokeEmitter;
    private ParticleEmitter trackDustEmitter;

    // Orbit camera
    private OrbitCamera orbitCam;

    // Debug collision wireframe
    private Geometry collisionWireframe;

    @Override
    public void simpleInitApp() {
        // 1. Orbit camera (left-drag rotate, scroll zoom, right-drag pan)
        flyCam.setEnabled(false);
        orbitCam = new OrbitCamera(cam, rootNode, inputManager);
        orbitCam.setDistance(15f);
        orbitCam.setLookAt(Vector3f.ZERO);

        // 2. Scene lighting (three-point)
        addLight(new DirectionalLight(
            new Vector3f(-0.5f, -0.8f, -0.3f).normalizeLocal(), ColorRGBA.White));
        addLight(new DirectionalLight(
            new Vector3f(0.3f, 0.4f, 0.8f).normalizeLocal(), ColorRGBA.White.mult(0.4f)));
        addLight(new AmbientLight(ColorRGBA.White.mult(0.3f)));

        // 3. Floor grid (reference plane)
        Geometry floor = createGridPlane(20, 20, 1);
        rootNode.attachChild(floor);

        // 4. Load model
        loadModel("heavy_tank.m3o");
    }

    private void loadModel(String path) {
        // Clear previous model
        if (modelRoot != null) modelRoot.removeFromParent();

        LoadedModel loaded = modelLoader.loadM3o(path, assetManager);
        modelRoot = loaded.spatial();
        manifest = loaded.manifest();

        rootNode.attachChild(modelRoot);

        // Resolve empty nodes
        turretPivotNode = manifest.getNode(NodeRole.TURRET_PIVOT);
        barrelPivotNode = manifest.getNode(NodeRole.BARREL_PIVOT);

        // Setup particle emitters at bone attach points
        setupParticleEmitters();

        // Create collision debug wireframe
        updateCollisionViz();

        // Update info panel
        updateModelInfo();
    }
}
```

### 18.4 Turret Preview Controls

```java
/**
 * TurretPreviewController rotates the turretPivot (Y-axis = yaw)
 * and barrelPivot (X-axis = pitch) in response to UI sliders/buttons.
 * When "Auto Idle Scan" is enabled, the turret sweeps slowly between
 * min/max idle scan angles, simulating in-game idle behavior.
 */
public class TurretPreviewController {

    private Node turretPivot;       // rotates around local Y → yaw
    private Node barrelPivot;       // rotates around local X → pitch
    private float currentYaw = 0f;
    private float currentPitch = 0f;

    private boolean autoScanEnabled;
    private float scanAngle = 0f;
    private float scanDirection = 1f;  // 1 = CW, -1 = CCW
    private float scanSpeed = 0.5f;    // rad/s
    private float minScanAngle = -45f * FastMath.DEG_TO_RAD;
    private float maxScanAngle = 45f * FastMath.DEG_TO_RAD;

    public void setYawTarget(float degrees) {
        // Snap turret pivot to exact angle
        currentYaw = degrees * FastMath.DEG_TO_RAD;
        applyRotation();
    }

    public void setPitchTarget(float degrees) {
        // Snap barrel pivot, clamped to manifest limits
        float minPitch = manifest().turret().minPitch();
        float maxPitch = manifest().turret().maxPitch();
        currentPitch = FastMath.clamp(
            degrees * FastMath.DEG_TO_RAD, minPitch, maxPitch);
        applyRotation();
    }

    public void update(float tpf) {
        if (autoScanEnabled && turretPivot != null) {
            scanAngle += scanDirection * scanSpeed * tpf;
            if (scanAngle > maxScanAngle) {
                scanAngle = maxScanAngle;
                scanDirection = -1f;
            } else if (scanAngle < minScanAngle) {
                scanAngle = minScanAngle;
                scanDirection = 1f;
            }
            turretPivot.setLocalRotation(
                new Quaternion().fromAngleAxis(scanAngle, Vector3f.UNIT_Y));
        }
    }

    private void applyRotation() {
        if (turretPivot != null) {
            turretPivot.setLocalRotation(
                new Quaternion().fromAngleAxis(currentYaw, Vector3f.UNIT_Y));
        }
        if (barrelPivot != null) {
            barrelPivot.setLocalRotation(
                new Quaternion().fromAngleAxis(currentPitch, Vector3f.UNIT_X));
        }
    }
}
```

### 18.5 Particle Preview — Muzzle Flash, Smoke, Dust

```java
/**
 * On "Fire Muzzle Flash" button press: emit a burst of particles
 * at the MUZZLE empty's world position, matching the in-game Gun Flash effect.
 * Engine smoke and track dust are toggle-able continuous emitters.
 */
public class ParticlePreview {

    public void fireMuzzleFlash(Node muzzleNode) {
        if (muzzleNode == null) return;

        // Burst: 5 particles, lifetime 0.1s, additive blend, yellow→transparent
        ParticleEmitter flash = new ParticleEmitter(
            "muzzleFlash", ParticleMesh.Type.Triangle, 5);
        flash.setLocalTranslation(muzzleNode.getWorldTranslation());
        flash.setStartColor(new ColorRGBA(1.0f, 0.8f, 0.2f, 1.0f));
        flash.setEndColor(new ColorRGBA(1.0f, 0.6f, 0.0f, 0.0f));
        flash.setStartSize(0.5f);
        flash.setEndSize(0.1f);
        flash.setLowLife(0.05f);
        flash.setHighLife(0.15f);
        flash.setParticlesPerSec(0);  // one-shot
        flash.getParticleInfluencer()
            .setInitialVelocity(new Vector3f(0, 1.0f, 0))
            .setVelocityVariation(0.2f);
        flash.setImagesX(1);
        flash.setImagesY(1);

        Material mat = new Material(assetManager,
            "Common/MatDefs/Misc/Particle.j3md");
        mat.setTexture("Texture",
            assetManager.loadTexture("textures/fx/flash.png"));
        mat.getAdditionalRenderState().setBlendMode(BlendMode.Additive);
        flash.setMaterial(mat);

        // Auto-remove after particles die
        flash.addParticleListener((ParticleListener) (emitter, count) -> {
            if (count == 0) emitter.removeFromParent();
        });

        rootNode.attachChild(flash);
        flash.emitAllParticles();
    }

    public void toggleEngineSmoke(Node smokeNode, boolean enable) { /* ... */ }
    public void toggleTrackDust(Node trackNode, boolean enable)  { /* ... */ }
}
```

### 18.6 Animation Preview

```java
/**
 * AnimPreviewController invokes named animations on the model's AnimControl.
 * Animation names come from ModelManifest (extracted during import).
 * Crossfades are used for smooth transitions between states.
 */
public class AnimationPreview {

    private AnimControl animControl;
    private Map<String, AnimationClip> clips;

    public void playAnimation(String clipName, float blendTime) {
        AnimationClip clip = clips.get(clipName);
        if (clip == null) {
            System.err.println("Animation not found: " + clipName);
            return;
        }
        AnimChannel channel = animControl.getChannel(0);
        channel.setAnim(clipName, blendTime);
        channel.setSpeed(clip.defaultSpeed());
        channel.setLoopMode(clip.looping() ? LoopMode.Loop : LoopMode.DontLoop);
    }

    public void stopAll() {
        animControl.getChannel(0).reset(false);
    }
}
```

### 18.7 Collision Visualization Overlay

```java
/**
 * Draws a wireframe overlay of the pre-baked collision shape:
 *   RadialShape → wireframe sphere
 *   RectShape   → wireframe box
 *
 * Rendered in a separate viewport with depth-test disabled
 * so it always shows through the model (X-ray effect).
 */
public void createCollisionWireframe(ModelManifest manifest) {
    CollisionShape shape = manifest.collisionShape();
    switch (shape) {
        case RadialShape r -> {
            // JME WireSphere or custom line geometry
            Geometry sphere = createWireSphere(r.radius(), 16);
            sphere.setLocalTranslation(r.center());
            collisionNode.attachChild(sphere);
        }
        case RectShape rect -> {
            Geometry box = createWireBox(rect.halfExtents());
            box.setLocalTranslation(rect.center());
            // Apply rotation if OBBox
            collisionNode.attachChild(box);
        }
    }
}
```

### 18.8 Model Info Panel (read-only stats)

```java
public void updateModelInfo(ModelManifest manifest, Node rootNode) {
    int vertexCount = countVertices(rootNode);
    int triangleCount = countTriangles(rootNode);
    int boneCount = manifest.animationClips().values().stream()
        .flatMapToInt(c -> c.tracks().stream().mapToInt(t -> 1)).sum();
    int animCount = manifest.animationClips().size();

    CollisionShape shape = manifest.collisionShape();
    String shapeInfo = switch (shape) {
        case RadialShape r -> String.format("Radius: %.1f", r.radius());
        case RectShape rect -> String.format("AABB: %.1f×%.1f×%.1f",
            rect.halfExtents().x * 2, rect.halfExtents().y * 2, rect.halfExtents().z * 2);
    };

    // Push to UI text labels
    infoPanel.setVertexCount(vertexCount);
    infoPanel.setTriangleCount(triangleCount);
    infoPanel.setBoneCount(boneCount);
    infoPanel.setAnimCount(animCount);
    infoPanel.setShapeInfo(shapeInfo);
}
```

---

## 20. Asset Pipeline Integration into Build (Gradle)

```kotlin
// build.gradle.kts — custom Gradle task for asset import

val blenderExecutable = project.findProperty("blender.path") ?: "blender"
val intermediateDir = layout.buildDirectory.dir("models/intermediate")
val finalDir = layout.projectDirectory.dir("assets/models/final")

tasks.register<JavaExec>("importModels") {
    group = "assets"
    description = "Export .blend → .glb → .m3o (importer tool)"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.jrts.tools.importer.BatchImporter")
    args = listOf(
        "--source", "assets/blender",
        "--intermediate", intermediateDir.get().asFile.absolutePath,
        "--output", finalDir.asFile.absolutePath,
        "--blender", blenderExecutable.toString()
    )
    // Only re-run if .blend files changed
    inputs.dir("assets/blender")
    outputs.dir(finalDir)
}

tasks.register<JavaExec>("previewModel") {
    group = "assets"
    description = "Launch unit preview app"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.jrts.tools.preview.UnitPreviewApp")
    // args = listOf("assets/models/final/heavy_tank.m3o")  // optional default
}

// Hook into build
tasks.named("processResources") {
    dependsOn("importModels")
}
```

---

## 21. UI System — Nifty GUI

### 20.1 Architecture

JME integrates with Nifty GUI via `NiftyJmeDisplay`. Each screen is a Nifty XML layout:

```
assets/ui/nifty/
├── main_menu.xml
├── skirmish_setup.xml
├── hud.xml              # in-game HUD
├── command_card.xml     # bottom command buttons
├── production_palette.xml
└── minimap.xml
```

### 20.2 In-Game HUD Layout

```
+-----------------------------------------------------------------------+
| top_panel (credits, power, player info)          | minimap container   |
+-----------------------------------------------------------------------+
|                                    |                                   |
|          3D GAME VIEW             |   right_panel                     |
|       (JME viewport)              |   (production palette,             |
|                                    |    build queue)                   |
+------------------------------------+-----------------------------------+
| unit_info (health, stats)        |  command_card (move/attack/stop)   |
+----------------------------------+------------------------------------+
```

### 20.3 HUDController.java

```java
public class HUDController {
    void updateResources(int credits, int power);
    void updateSelection(Entity[] selected);
    void showCommandCard(Entity[] selected);  // context-sensitive actions
    void updateProductionQueue(ProductionItem[] queue);
    void showMinimapPing(Vector3f worldPos);
}
```

---

## 22. Minimap

### 21.1 MinimapRenderer

JPEG-textured quad rendered in a separate JME viewport (or Nifty image):

```java
public class MinimapRenderer {
    Image minimapImage;    // dynamically updated texture
    Camera minimapCam;     // top-down ortho camera

    void update() {
        // Render terrain, structures, units as colored dots
        // Overlay fog-of-war darkness
        // Draw selection rectangle, attack-move lines
        // Click-to-navigate: translate screen pos → world coords
    }
}
```

---

## 23. Implementation Phases

### Phase 0: Project Scaffold (Week 1)
- Gradle project setup with JME dependencies (desktop LWJGL 3)
- `Main.java` extending `SimpleApplication`, basic app lifecycle
- ECS skeleton: Entity, EntityManager, Component, System base class
- Flat terrain plane (1×1 km quad with repeating texture)
- Basic RtsCamera (pan, zoom, rotate)
- TOML loader with `toml4j` dependency

### Phase 1: Asset Import Pipeline (Week 2)
- GltfImporter: load .glb via JME's GltfLoader
- EmptyNodeResolver: classify nodes by naming convention (case-insensitive regex)
- CollisionBaker: auto-compute AABB/radius from Chassis mesh vertices
- M3oExporter: serialize Node + ModelManifest to .m3o binary format
- BatchImporter CLI: walk assets/blender/ → invoke Blender CLI → import → .m3o
- Gradle `importModels` task integration (incremental: hash-based skip)
- Import contract validation (mandatory empty nodes per category)

### Phase 2: Unit Preview App (Week 2-3)
- UnitPreviewApp: standalone JME SimpleApplication
- Orbit camera (left-drag rotate, scroll zoom, right-drag pan)
- Three-point lighting + reference floor grid
- TurretPreviewController: yaw slider, pitch slider, auto-idle-scan toggle
- ParticlePreview: muzzle flash burst, engine smoke continuous, track dust continuous
- AnimationPreview: cycle through all imported animation clips with crossfade
- Collision wireframe overlay (toggle on/off)
- Model info panel (vertex/tri count, bone count, anim count, collision bounds)
- File-picker dialog for selecting .m3o files
- Launch via Gradle: `./gradlew previewModel`

### Phase 3: Unit Spawning & Rendering (Week 3-4)
- ModelLoader: load .m3o files at runtime, resolve empty-node hierarchy
- UnitTemplate + config loading (TOML)
- EntityFactory: create entity from UnitTemplate + LoadedModel
- SpatialComponent: link entity → JME Spatial with UserData(entityId)
- Blender test assets (one tank, one infantry, one building)
- Unit placement on terrain, basic Transform rendering

### Phase 4: Movement & Pathfinding (Week 4-5)
- LocomotorComponent + LocomotorTemplate loading
- GridMap: terrain grid with cell flags
- Pathfinder: A* with Octile heuristic
- MovementSystem: acceleration/turn/position update per frame
- Basic MODELCONDITION_MOVING → animation

### Phase 5: Collision System (Week 5-6)
- CollisionShape (Radial + Rect) with SAT and circle math
- CollisionSystem: grid-based broad phase + narrow phase
- Ground clamping (Z = terrainHeight)
- Unit repulsion (soft push out of SOFT_BLOCKED cells)

### Phase 6: Selection & Input (Week 6-7)
- RtsInputHandler: mouse/keyboard mapping
- SelectionBox: screen-space drag rectangle rendering
- SelectionSystem: single-click, drag-select, shift-add, control groups
- CommandDispatcher: move orders, right-click context

### Phase 7: Weapon System (Week 7-9)
- WeaponTemplate + WeaponWeaponSet data loading
- WeaponStateMachine (READY → PRE_ATTACK → FIRE → BETWEEN_SHOTS → RELOAD)
- Hitscan weapon (instant damage application)
- Projectile weapon (spawn ProjectileEntity, fly, impact)
- DamageCalculator: armor table, range falloff, accuracy roll
- CollideMask filtering

### Phase 8: Turret System (Week 9-10)
- TurretComponent with JME bone reference
- TurretStateMachine (IDLE → AIM → FIRE → HOLD → RECENTER)
- Bone rotation for turret (+Y) and barrel (+X)
- Idle scanning animation

### Phase 9: Docking & Resources (Week 10-11)
- DockComponent + DockStateMachine
- GatherComponent + GatherStateMachine
- PlayerResources (credits, power)
- SupplyWarehouse ↔ Harvester ↔ Refinery cycle
- HealDock and RepairDock actions

### Phase 10: Death & Destruction (Week 11)
- DeathSystem: trigger MODELCONDITION_DYING
- DeathType-specific effects (EXPLODED → explosion particles)
- Entity cleanup (remove from ECS + scene graph)
- Debris/rubble spawning for structures

### Phase 11: Particle Effects (Week 11-12)
- ParticleTemplateData loading from TOML
- JME ParticleEmitter integration
- Effect trigger mapping (fire → flash+smoke, impact → spark, death → smoke)
- Camera shake on explosions

### Phase 12: UI/HUD (Week 12-13)
- Nifty GUI integration
- Main menu screen (campaign/skirmish/settings)
- In-game HUD: resource bar, minimap, command card, production palette, unit info
- Minimap with fog-of-war overlay

### Phase 13: Polish & Integration (Week 13-14)
- Fog-of-war system (shroud/reveal)
- Sound integration (JME AudioNode)
- Control groups (Ctrl+1..9)
- Camera bookmarks (F1..F4)
- Quick-test mode: 1-click flat map with spawn commands
- Build queue and production system
- Veterancy system stub

### Phase 14: Testing & Optimization (Week 14-15)
- Unit tests for collision math, pathfinding, damage calc, state machines
- Performance: spatial hash for collision broad phase, entity pooling
- Profile and optimize entity queries (bitmask matching)
- Benchmark: 200 units + 50 buildings at 60 FPS target

---

## 24. Critical JME-Specific Implementation Details

### 23.1 Coordinate System

JME uses **Y-up** right-handed. In most RTS literature, Z is up. Our convention:
- **JME World X** → East
- **JME World Y** → Up (height/altitude)
- **JME World Z** → South

All config values in XY (top-down) must translate to JME's XZ plane:
```java
Vector3f worldPos = new Vector3f(configX, terrainHeight, configZ);
```

### 23.2 Raycasting for Terrain Picking

```java
// JME's CollisionResults for picking terrain
CollisionResults results = new CollisionResults();
Ray ray = new Ray(cam.getLocation(), cam.getDirection());
terrainNode.collideWith(ray, results);
if (results.size() > 0) {
    Vector3f hitPoint = results.getClosestCollision().getContactPoint();
}
```

### 23.3 Entity ↔ Scene Graph Lifecycle

```java
// EntityManager.create():
//   1. Acquire entity ID from pool
//   2. Load model Spatial from AssetManager
//   3. Attach Spatial to scene's rootNode
//   4. Store entity ID in Spatial's UserData
//   5. Return Entity

// EntityManager.destroy(entity):
//   1. Remove Spatial from scene's rootNode
//   2. Remove all components
//   3. Return entity ID to pool
```

### 23.4 Threading Model

JME 3.6 uses a single-threaded update loop by default. All ECS systems run on this thread:
```java
@Override
public void simpleUpdate(float tps) {
    // All systems updated sequentially on main thread
    world.update(tps);
}
```

For intensive work (pathfinding for many units), consider a `PathfindingSystem` with a worker thread that returns `CompletableFuture<PathResult>`, with results consumed on the main thread.

### 23.5 AssetManager Caching

JME's `AssetManager` caches loaded assets by key. Unit models loaded once:
```java
assetManager.loadModel("models/units/heavy_tank.glb"); // cached
```

---

## 25. Key Dependencies (build.gradle.kts)

```kotlin
dependencies {
    // JMonkeyEngine
    implementation("org.jmonkeyengine:jme3-core:3.6.1-stable")
    implementation("org.jmonkeyengine:jme3-desktop:3.6.1-stable")
    implementation("org.jmonkeyengine:jme3-lwjgl3:3.6.1-stable")
    implementation("org.jmonkeyengine:jme3-plugins:3.6.1-stable")
    implementation("org.jmonkeyengine:jme3-effects:3.6.1-stable")
    implementation("org.jmonkeyengine:jme3-niftygui:3.6.1-stable")

    // TOML config
    implementation("com.moandjiezana.toml:toml4j:0.7.2")

    // JSON for savegames
    implementation("com.google.code.gson:gson:2.10.1")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}
```

---

## 26. Risk Assessment & Mitigation

| Risk | Severity | Mitigation |
|---|---|---|
| Blender CLI changes break export automation | Medium | Pin to a known-good Blender version; version-check in BatchImporter; fallback to pre-exported .glb in `intermediate/` |
| JME glTF loader emission of empty axes differs between Blender versions | Medium | Unit-test EmptyNodeResolver against golden .glb files; validate NodeRole resolution in CI |
| .m3o binary format version mismatch after model re-import | Low | Embed format version number; ModelLoader rejects mismatched versions with clear error; auto-reimport in dev mode |
| Model contract violations (artist misses mandatory empty node) | Medium | Strict validation in CollisionBaker; BatchImporter fails build on errors; generate HTML report showing exactly which nodes are missing |
| Large .m3o files increase load times | Low | Use JME's AssetManager background loading; pre-warm cache; optional Draco compression in Blender export |
| Preview app camera controls conflict with in-game RtsCamera patterns | Low | Preview app uses a dedicated OrbitCamera, completely separate from RtsCamera code path |
| JME glTF importer doesn't handle empty axes correctly | Low | JME 3.6+ glTF loader supports empty nodes natively (glTF Node with no mesh = JME Node with no Geometry). Verified against spec. |
| Nifty GUI integration complexity | Medium | Keep HUD minimal initially; use JME `Picture` + `BitmapText` as fallback for basic overlays |
| A* performance on large maps with many units | Medium | Use worker thread for pathfinding; cache common paths; implement hierarchical pathfinding if needed (Phase 14) |
| CollisionSystem O(n²) with many projectiles | Low | Spatial hash broad phase from day one; projectiles are short-lived |
| Particle count exceeding GPU budget | Low | Particle pool with hard max per emitter; LOD for distant particles |
| Memory leaks from ECS entity/spatial lifecycle | Low | Write leak-detection tests; use `WeakReference` for spatial→entity backlinks |

---

## 27. Testing Strategy

### Unit Tests
- `EmptyNodeResolver`: classification of all NodeRoles from golden .glb (case-insensitive, partial match)
- `CollisionBaker`: verify AABB matches expected values from known meshes; verify radius derived correctly from AABB
- `M3oExporter` / `ModelLoader`: round-trip test — export .m3o, reload, assert manifest matches
- `CollisionDetection`: all shape-pair combinations, edge cases (radius=0, overlapping, just-touching)
- `Pathfinder`: known grid layouts, verify path cost, verify waypoint optimization
- `DamageCalculator`: verify armor table lookup, range falloff formula, accuracy roll distribution
- `WeaponStateMachine`: all state transitions, clip reload, out-of-ammo edge cases
- `TurretStateMachine`: angle math, pitch clamping, recentering behavior

### Integration Tests
- Entity spawn → render → select → move → destroy lifecycle
- Harvester resource cycle: source → refinery, credits increment correctly
- Turret aims and fires at moving target (angle tracking test)
- Weapon with splash damage hits multiple units in radius

### Manual Testing Tools
- Quick-test mode: launch with flat terrain, dev console to spawn any unit/weapon
- `/spawn HeavyTank` console command
- `/debugPaths` to render A* waypoints as lines
- `/debugCollision` to render collision shapes
