# Stage 1 Implementation Plan — Camera, Unit Import, Unit Selection, Simple Scene

## 0. Key implementation principles

* Remember that you should follow SOLID principles, don't create any "God Classes"
* Add proper comments to your code
* Think about covering code with tests using JUnit/Mockito or specific approaches for JMonkey if there are any
* Add proper logging at all levels, this will simplify issue debug in future
* Always use buffered I/O (readers, streams) if you need to read any files in classical Java way

## 1. Stage 1 Objectives

Deliver a minimal but functional slice of the engine:

| Deliverable | Acceptance Criteria |
|---|---|
| **RTS Camera** | Pan (WASD/edge-scroll), zoom (scroll wheel), rotate (middle-mouse drag), screen↔terrain coordinate mapping |
| **Unit Importer** | `.blend → .glb → .m3o` pipeline: GltfImporter, EmptyNodeResolver, CollisionBaker, M3oExporter, BatchImporter CLI |
| **Unit Previewer** | Standalone JME app with orbit camera, turret yaw/pitch controls, muzzle-flash particle test, mesh info overlay |
| **Unit Selection** | Single-click select (ray-pick), drag-rectangle select, Shift-add, selected-unit highlight, control group stubs |
| **Mock Navigation** | Right-click → straight-line path (no A* yet), tank moves at constant speed, rotates body toward heading |
| **Turret Rotation** | TurretPivot rotates toward mouse cursor (follows ground intersection), yaw-only in Stage 1 |
| **TOML Config** | Load `heavy_tank_example.toml`, `weapon_example.toml`, `building_example.toml` as typed Java objects |
| **Simple Scene** | Flat 100×100-unit terrain quad, sky color, directional light, one HeavyTank spawned at origin |

---

## 2. Architecture & SOLID Design

### 2.1 Package Map (Stage 1 subset)

```
com.jrts/
├── Main.java                         // JME SimpleApplication, boot
├── camera/
│   ├── RtsCamera.java               // Camera controller (pan/zoom/rotate)
│   ├── RtsCameraInputListener.java  // Maps input actions → camera
│   └── ScreenMap.java               // Screen ↔ World ↔ Terrain coordinate transforms
├── input/
│   ├── ActionMapper.java            // Maps AWT keys/mouse to named actions
│   ├── SelectionBox.java            // Drag-rectangle state + rendering
│   ├── SelectionSystem.java         // Select/deselect/query logic
│   ├── CommandDispatcher.java       // Right-click → order dispatch
│   └── MousePicker.java             // JME raycasting for object + terrain
├── config/
│   ├── ConfigLoader.java            // TOML file → typed data objects
│   ├── UnitConfig.java              // Maps heavy_tank_example.toml [identity]+[stats]+...
│   ├── WeaponConfig.java            // Maps weapon_example.toml [weapon]
│   ├── BuildingConfig.java          // Maps building_example.toml
│   └── LocomotorConfig.java         // Embedded [movement] section
├── unit/
│   ├── Unit.java                    // Runtime unit (id, config ref, spatial, flags, health)
│   ├── UnitFlags.java               // Bitmask enum (CAN_MOVE, SELECTABLE, IS_STRUCTURE, ...)
│   ├── UnitFactory.java             // config + loaded model → Unit instance
│   ├── UnitRegistry.java            // All live units, lookup by id / spatial
│   └── ArmorType.java               // Enum: none, wood, light, heavy, concrete
├── movement/
│   ├── NavigationService.java       // Interface: computePath(start, end)
│   ├── SimpleLineNavigation.java    // Mock impl: returns straight-line waypoints
│   ├── MovementController.java      // Per-frame: accelerate, turn toward waypoint, update pos
│   └── LocomotorProfile.java        // Runtime values from LocomotorConfig
├── turret/
│   ├── TurretController.java        // Rotates TurretPivot Node toward target point
│   └── TurretConfig.java            // From [turrets] section
├── selectable/
│   ├── SelectableComponent.java     // Marker + highlight geometry
│   └── SelectionHighlight.java      // Circle/box drawn under selected units
├── scene/
│   ├── SceneBootstrapper.java       // Creates terrain, light, sky, spawns units
│   ├── TerrainHeightProvider.java   // Interface: getHeight(x,z), isWater(), getGradient()
│   ├── FlatTerrainHeightProvider.java// Stage 1 flat terrain impl, returns constant 0
│   └── TerrainPlane.java            // Flat Quad mesh, configurable size (render-only)
├── tools/
│   ├── importer/
│   │   ├── GltfImporter.java        // .glb → JME Node tree
│   │   ├── EmptyNodeResolver.java   // Detect TurretPivot, BarrelPivot, Muzzle, etc.
│   │   ├── CollisionBaker.java      // Auto-compute AABB / radius from mesh
│   │   ├── M3oExporter.java         // Serialize Node + manifest to .m3o
│   │   ├── BatchImporter.java       // CLI: walk .blend files, export, import, validate
│   │   └── ModelManifest.java       // DTO: empty nodes, collision, animations
│   └── preview/
│       ├── UnitPreviewApp.java      // Standalone JME SimpleApplication
│       ├── OrbitCamera.java         // Left-drag rotate, scroll zoom, right-drag pan
│       ├── TurretPreviewController.java
│       ├── ParticlePreviewEmitter.java
│       └── CollisionWireframe.java  // Debug overlay for AABB/radius
├── rendering/
│   └── ModelLoader.java             // .m3o → LoadedModel (spatial + manifest)
└── util/
    ├── BitMask32.java               // 32-bit flag wrapper, read/write/test
    └── OctileDistance.java          // Heuristic helper (stub for later A*)
```

### 2.2 SOLID Principles Mapped

| Principle | Application in Stage 1 |
|---|---|
| **S**ingle Responsibility | Each class does exactly one thing: `MovementController` only updates position; `SelectionSystem` only tracks selected set; `EmptyNodeResolver` only classifies nodes |
| **O**pen/Closed | `NavigationService` is an interface → mock `SimpleLineNavigation` today, real `AStarNavigation` later without changing callers |
| **L**iskov Substitution | All `NavigationService` impls must produce valid `List<Vector3f>` waypoints for any start/end; no impl may throw for valid input |
| **I**nterface Segregation | Small focused interfaces: `CameraControl` (update), `GroundRaycaster` (screen→terrain), `Pickable` (ray-intersect); no fat interfaces |
| **D**ependency Inversion | `MovementController` depends on `NavigationService` interface, not concrete class; `UnitFactory` depends on `ModelLoader` interface; `SelectionSystem` issues `SelectionEvent` to observers |

### 2.3 No God Classes — Class Size Limits

| Max Lines | Rationale |
|---|---|
| ~200 | Data classes (config DTOs, records) |
| ~300 | Single-responsibility services (MovementController, TurretController) |
| ~400 | Complex but focused (GltfImporter, EmptyNodeResolver) |
| ~500 | Framework glue (Main.java, SceneBootstrapper) — split if larger |

---

## 3. Detailed Subsystem Designs

### 3.1 Camera Subsystem

#### 3.1.1 RtsCamera.java

```java
/**
 * Tactical RTS camera wrapping JME's Camera.
 *
 * Coordinate convention:
 *   JME world X → East (right on screen)
 *   JME world Y → Up (height above terrain)
 *   JME world Z → South (up on screen in default view)
 *
 * The camera orbits a fixed pivot point at ground level.
 * Camera position is derived from pivot + spherical offsets:
 *   pitch: angle above horizon (0 = edge-on, PI/2 = top-down)
 *   yaw: compass rotation around Y axis
 *   distance: zoom level (height above pivot)
 */
public class RtsCamera {

    private final Camera cam;
    private Vector3f pivot;           // ground point camera orbits around
    private float pitch;              // radians, default ~55° (0.96 rad)
    private float yaw;                // radians, default 0 (North-East)
    private float distance;           // world units from pivot to camera

    // Clamps (from spec Section 10.3)
    private static final float MIN_HEIGHT = 20f;
    private static final float MAX_HEIGHT = 500f;
    private static final float MIN_PITCH = 0.1f;    // never fully edge-on
    private static final float MAX_PITCH = 1.4f;    // ~80°, never fully top-down

    // Pan/zoom/rotate speeds
    private float panSpeed = 0.5f;            // units per pixel at distance=1
    private float zoomSpeed = 10f;            // units per scroll tick
    private float rotateSpeed = 1.0f;         // radians per pixel dragged
    private int scrollEdgeSize = 10;          // pixels from screen edge

    /**
     * Recalculates cam location and look direction from pivot + pitch + yaw + distance.
     * Called every frame after input modifies any parameter.
     */
    public void update(float tpf) {
        // camPos = pivot + sphericalToCartesian(distance, pitch, yaw)
        // cam.lookAt(pivot, Vector3f.UNIT_Y)
    }

    // --- Input-driven mutators (called by RtsCameraInputListener) ---
    public void pan(float dx, float dy) { /* ... */ }
    public void zoom(float amount)       { /* distance -= amount * zoomSpeed, clamp */ }
    public void rotate(float amount)     { /* yaw += amount * rotateSpeed */ }
    public boolean isMouseAtEdge(int mouseX, int mouseY, int screenW, int screenH) { /* ... */ }
}
```

#### 3.1.2 ScreenMap.java

```java
/**
 * Encapsulates all screen ↔ world ↔ terrain coordinate conversions.
 * Stateless helper — no dependencies.
 *
 * All terrain queries go through TerrainHeightProvider, never a mesh.
 * This keeps the collision and rendering models completely separate.
 */
public final class ScreenMap {

    /**
     * Screen pixel → terrain world position.
     * Returns the terrain intersection point with the camera ray.
     * Uses terrainProvider.getHeight() for the Y value.
     *
     * @param screenX  screen pixel X
     * @param screenY  screen pixel Y
     * @param cam      current camera state
     * @param terrain  height provider for ground Y
     * @return world position on terrain surface, or null if ray misses
     */
    public static Vector3f screenToTerrain(float screenX, float screenY, Camera cam, TerrainHeightProvider terrain);

    /**
     * World position → screen pixel.
     */
    public static Vector2f worldToScreen(Vector3f worldPos, Camera cam, int screenW, int screenH);

    /**
     * Clamp a world position to stay within map bounds (uses terrain provider).
     */
    public static Vector3f clampToMap(Vector3f pos, TerrainHeightProvider terrain);
}
```

#### 3.1.3 RtsCameraInputListener.java

```java
/**
 * Bridges JME's raw InputManager events to RtsCamera method calls.
 * Registered as a RawInputListener or AnalogListener.
 *
 * Key bindings (from spec Section 10.2):
 *   W/A/S/D or Arrow keys → pan
 *   Mouse at screen edge (< 10px) → auto-scroll
 *   Right mouse drag → reverse drag-scroll (camera pans opposite to drag)
 *   Middle mouse drag → rotate yaw
 *   Scroll wheel → zoom
 *   Ctrl+F1..F4 → save bookmark (stub for Stage 1)
 *   F1..F4 → restore bookmark (stub)
 */
public class RtsCameraInputListener implements AnalogListener, ActionListener {

    private final RtsCamera camera;
    private final int screenWidth, screenHeight;

    @Override
    public void onAnalog(String name, float value, float tpf)    { /* dispatch */ }
    @Override
    public void onAction(String name, boolean isPressed, float tpf) { /* dispatch */ }
}
```

#### 3.1.4 Camera Tests

```java
// RtsCameraTest.java
@Test void panMovesPivotInViewPlane()       { /* ... */ }
@Test void zoomChangesDistanceWithinClamps() { /* ... */ }
@Test void rotateModifiesYawModulo2pi()      { /* ... */ }
@Test void updateDerivesCorrectCamPosition() { /* spherical → Cartesian math verified */ }

// ScreenMapTest.java
@Test void screenToTerrainOnFlatPlane() { /* known cam → ray intersect → picks expected XZ, Y from FlatTerrainHeightProvider */ }
@Test void screenToTerrainOutsidePlane() { /* ray misses Y=0 → returns null */ }
@Test void screenToTerrainUsesTerrainProvider() { /* mock terrain returns custom height → verify Y equals that height */ }
@Test void worldToScreenRoundtrip() { /* world→screen→terrain ≈ original world (XZ only) */ }
```

---

### 3.2 Config Loading (TOML)

#### 3.2.1 Inheritance vs Composition Approach

Config classes use **composition over inheritance**. Each TOML section becomes a standalone record/class. The `UnitConfig` composes them:

```java
/**
 * Full unit definition parsed from heavy_tank_example.toml (and similar).
 *
 * Each [section] in TOML maps to a separate composed object.
 * No inheritance — a building and a tank both use UnitConfig but
 * differ in which sub-objects are present (flag: isStructure).
 */
public record UnitConfig(
    IdentitySection identity,
    StatsSection stats,
    List<String> prerequisites,       // from [prerequisites]
    int buildingsRequired,            // from [prerequisites]
    VeterancySection veterancy,       // optional stub
    CombatSection combat,
    AbilitiesSection abilities,
    PassengersSection passengers,
    AudioSection audio,
    MovementSection movement,
    TurretsSection turrets,
    BuildingInteractionsSection buildingInteractions,
    SpecialFlagsSection specialFlags
) {
    /** @return true if this config describes a structure/building */
    public boolean isStructure() {
        return "building".equalsIgnoreCase(identity.type());
    }
}
```

#### 3.2.2 Sub-Section Records

```java
public record IdentitySection(
    String name,
    String displayName,
    String category,          // Soldier, AFV, IFV, Structure, etc.
    String owner,             // Republic, Civilian
    boolean selectable,
    // ... all booleans from [identity]
    String type               // "unit" or "building"
) {}

public record StatsSection(
    int strength,             // hit points (or "health" for buildings)
    String armor,             // none, wood, light, heavy, concrete
    int sight,                // sight range in cells
    float guardRange,         // defaults to weapon range
    float speed,
    float rot,                // body turn rate (deg/s)
    int cost,
    int points,
    int techLevel,
    int buildLimit,
    float buildTime
) {}

public record MovementSection(
    String locomotor,         // tracks, wheels, walker, hover
    boolean crushable,
    boolean crusher,
    boolean carriesCrate
) {}

public record TurretsSection(
    boolean turret,           // has turret
    boolean turretSpins,      // idle spin
    List<Float> turretRotationYaw,   // [-180, 180]
    float turretRotationSpeed,       // deg/s
    List<Float> barrelElevationPitch, // [-5, 45]
    float barrelSpeed
) {}

// CombatSection, AbilitiesSection, etc. follow same pattern.
```

#### 3.2.3 ConfigLoader.java

```java
/**
 * Reads .toml files and returns typed configuration objects.
 * Delegates to toml4j for raw parsing, then maps to records.
 *
 * Single public entry point per config type.
 * All parsing errors thrown as ConfigParseException with line reference.
 */
public class ConfigLoader {

    private final Path configDir;   // e.g. assets/config/

    public UnitConfig loadUnitConfig(String unitName);     // reads units/{unitName}.toml
    public WeaponConfig loadWeaponConfig(String weaponName); // reads weapons/{weaponName}.toml
    public BuildingConfig loadBuildingConfig(String buildingName); // reads buildings/{name}.toml

    /** Batch load all unit configs found in configDir/units/ */
    public Map<String, UnitConfig> loadAllUnitConfigs();
}
```

#### 3.2.4 Config Tests

```java
// ConfigLoaderTest.java
@Test void loadsHeavyTankConfigCorrectly() {
    UnitConfig cfg = loader.loadUnitConfig("heavy_tank");
    assertEquals("HeavyTank", cfg.identity().name());
    assertEquals(400, cfg.stats().strength());
    assertEquals("heavy", cfg.stats().armor());
    assertEquals(5.0f, cfg.stats().speed(), 0.001f);
    assertTrue(cfg.turrets().turret());
    assertEquals("125mm_cannon", cfg.combat().primaryWeapon());
}

@Test void loadsWeaponConfigCorrectly() {
    WeaponConfig cfg = loader.loadWeaponConfig("artillery_cannon");
    assertEquals("155mm_howitzer", cfg.name());
    assertEquals(150.0f, cfg.damage(), 0.001f);
}

@Test void loadsBuildingConfigCorrectly() {
    BuildingConfig cfg = loader.loadBuildingConfig("war_factory");
    assertEquals(1200, cfg.stats().health());  // note: building uses "health" not "strength"
}

@Test(expected = ConfigParseException.class)
void missingFileThrowsConfigParseException() { /* ... */ }

@Test void batchLoadReturnsAllUnits() { /* ... */ }
```

---

### 3.3 Unit System (Runtime)

#### 3.3.1 Unit.java

```java
/**
 * Runtime unit instance. Not an ECS entity yet — that comes in Stage 2.
 * For Stage 1, Unit is a plain object holding all state needed for
 * camera + selection + movement + turret.
 *
 * The JME Spatial is the visual representation. Unit wraps it
 * and provides game-level state (health, flags, selection status).
 */
public class Unit {

    private final int id;                                // unique runtime id
    private final UnitConfig config;                     // immutable template reference
    private final Node spatial;                          // JME scene-graph node
    private final ModelManifest manifest;                // empty node references
    private final int flags;                             // UnitFlags bitmask

    // Mutable state
    private Vector3f position;                           // world position (delegates to spatial)
    private float bodyYaw;                               // chassis facing direction (radians)
    private boolean selected;
    private int health;
    private List<Vector3f> waypoints;                    // active movement waypoints

    // Resolved from manifest
    private Node turretPivotNode;                        // for turret rotation
    private Node muzzleNode;                             // for future FX

    public Unit(int id, UnitConfig config, Node spatial, ModelManifest manifest) {
        // Constructor: set flags from config booleans, cache empty-node references
    }

    // --- Accessors ---
    public int id()                    { return id; }
    public UnitConfig config()         { return config; }
    public Node spatial()             { return spatial; }
    public Vector3f position()        { return spatial.getWorldTranslation(); }
    public float bodyYaw()            { return bodyYaw; }
    public boolean isSelected()       { return selected; }
    public void setSelected(boolean s) { selected = s; }
    public Node turretPivot()         { return turretPivotNode; }
    public Node muzzle()              { return muzzleNode; }

    // Flag checks (delegate to UnitFlags bitmask)
    public boolean canMove()          { return (flags & UnitFlags.CAN_MOVE) != 0; }
    public boolean isSelectable()     { return (flags & UnitFlags.SELECTABLE) != 0; }
    public boolean hasTurret()        { return (flags & UnitFlags.HAS_TURRET) != 0; }

    // Waypoints (for movement)
    private List<Vector3f> waypoints;
    public void setWaypoints(List<Vector3f> wps) { this.waypoints = wps; }
    public List<Vector3f> getWaypoints() { return waypoints; }

    /** @return height above terrain (0 for ground units, >0 for hover/airborne) */
    public float getPreferredHeight() {
        return (flags & UnitFlags.AIRBORNE) != 0 ? 2.0f : 0.0f;
    }
}
```

#### 3.3.2 UnitFlags.java

```java
/**
 * 32-bit flag constants matching the spec's UnitFlags enum.
 * Each flag is a power-of-two bit.
 */
public final class UnitFlags {
    private UnitFlags() {}

    public static final int CAN_MOVE         = 0x0001;
    public static final int CAN_BE_SELECTED  = 0x0002;
    public static final int CAN_BE_PICKED    = 0x0004;
    public static final int IS_STRUCTURE     = 0x0008;
    public static final int IS_PROJECTILE    = 0x0010;
    public static final int SELECTABLE       = 0x0020;
    public static final int AIRBORNE         = 0x0100;
    public static final int DOCKABLE         = 0x0200;
    public static final int CAN_BUILD        = 0x0400;
    public static final int CAN_GATHER       = 0x0800;
    public static final int HAS_TURRET       = 0x1000;

    /** Build a flags bitmask from a UnitConfig. */
    public static int fromConfig(UnitConfig config) {
        int f = 0;
        IdentitySection id = config.identity();
        if (!config.isStructure()) f |= CAN_MOVE;
        if (id.selectable())      f |= CAN_BE_SELECTED | CAN_BE_PICKED | SELECTABLE;
        if (config.isStructure()) f |= IS_STRUCTURE;
        if (config.turrets() != null && config.turrets().turret()) f |= HAS_TURRET;
        // AIRBORNE, DOCKABLE, etc. added in later stages
        return f;
    }
}
```

#### 3.3.3 UnitFactory.java

```java
/**
 * Creates a Unit from a UnitConfig and a loaded 3D model.
 *
 * Responsibilities:
 *   - Assign a unique runtime ID
 *   - Compute UnitFlags from config
 *   - Resolve empty-node references from ModelManifest
 *   - Set initial position and rotation on the spatial
 *   - Register the unit in UnitRegistry
 */
public class UnitFactory {

    private final UnitRegistry registry;
    private final AtomicInteger nextId = new AtomicInteger(1);

    /**
     * @param config   parsed TOML unit config
     * @param model    loaded .m3o (spatial + manifest)
     * @param position initial world position
     * @param yaw      initial body facing (radians)
     * @return a new, live Unit
     */
    public Unit create(UnitConfig config, LoadedModel model, Vector3f position, float yaw) {
        int id = nextId.getAndIncrement();
        int flags = UnitFlags.fromConfig(config);
        Node spatial = model.spatial();
        spatial.setLocalTranslation(position);
        // rotation around JME Y axis (up) for body yaw
        spatial.setLocalRotation(new Quaternion().fromAngleAxis(yaw, Vector3f.UNIT_Y));

        // Attach to scene root
        Unit unit = new Unit(id, config, spatial, model.manifest());
        registry.register(unit);
        return unit;
    }
}
```

#### 3.3.4 UnitRegistry.java

```java
/**
 * Central registry of all live units. Provides:
 *   - O(1) lookup by runtime ID
 *   - O(1) lookup by JME Spatial (via UserData back-link)
 *   - Iteration over all units
 *   - Iteration filtered by flag mask
 */
public class UnitRegistry {

    private final Map<Integer, Unit> byId = new HashMap<>();
    private final Map<Spatial, Unit> bySpatial = new IdentityHashMap<>();

    public void register(Unit unit);
    public void unregister(Unit unit);

    public Optional<Unit> findById(int id);
    public Optional<Unit> findBySpatial(Spatial spatial);
    public List<Unit> allUnits();
    public List<Unit> unitsWithFlag(int flag);  // e.g. SELECTABLE
    public int count();
}
```

#### 3.3.5 Unit Tests

```java
// UnitFlagsTest.java
@Test void fromConfig_tankSetsMoveSelectedTurret() { /* CAN_MOVE | SELECTABLE | HAS_TURRET */ }
@Test void fromConfig_buildingSetsStructure()       { /* IS_STRUCTURE, no CAN_MOVE */ }

// UnitRegistryTest.java
@Test void registerAndFindById()       { /* ... */ }
@Test void findBySpatialAfterRegister() { /* ... */ }
@Test void unregisterRemovesFromBothMaps() { /* ... */ }

// UnitFactoryTest.java
@Test void createSetsCorrectPosition() {
    Unit u = factory.create(tankConfig, loadedModel, new Vector3f(10, 0, 5), 0.5f);
    assertEquals(new Vector3f(10, 0, 5), u.position());
    assertEquals(0.5f, u.bodyYaw(), 0.001f);
}
```

---

### 3.4 Unit Import Subsystem (Build-Time Tools)

#### 3.4.1 GltfImporter.java

```java
/**
 * Stage 1 of the import pipeline.
 * Reads a .glb (glTF binary) file using JME's GltfLoader
 * and returns a flat Node tree with attached Geometry.
 *
 * This is a BUILD-TIME tool. It does NOT run at game runtime.
 *
 * Errors:
 *   - File not found → ImportException
 *   - Invalid glTF → ImportException (wraps JME exception)
 *   - Empty scene (no meshes) → ImportException
 */
public class GltfImporter {

    /**
     * @param glbPath   path to .glb file
     * @param assetManager  JME AssetManager (configured for build-time paths)
     * @return root Node of the loaded scene
     * @throws ImportException on any failure
     */
    public Node importGlb(Path glbPath, AssetManager assetManager) throws ImportException;

    /**
     * Validates the loaded node tree:
     *   - At least one Geometry descendant (not just empties)
     *   - No more than one skeleton armature (multi-armature unsupported)
     *
     * @return list of validation warnings (non-fatal); throws on fatal issues
     */
    public List<String> validate(Node rootNode) throws ImportException;
}
```

#### 3.4.2 EmptyNodeResolver.java

```java
/**
 * Stage 2 of the import pipeline.
 * Walks the JME Node tree and classifies nodes by their name
 * against the Blender naming convention (case-insensitive regex match).
 *
 * Detected node roles are stored in a ModelManifest.
 * A single node can match at most one role.
 *
 * The classification is order-dependent: more specific patterns
 * checked before general ones (e.g. TurretPivot before Turret).
 */
public class EmptyNodeResolver {

    /**
     * @param rootNode  the root of the imported glTF tree
     * @return populated ModelManifest with all matched roles
     */
    public ModelManifest resolve(Node rootNode);

    /**
     * Standard naming patterns from spec Section 14.
     * Order matters — specific before general.
     */
    public enum NodeRole {
        TURRET_PIVOT,    // matches *TurretPivot* (case-insensitive)
        BARREL_PIVOT,    // matches *BarrelPivot*
        MUZZLE,          // matches *Muzzle* (but not *MuzzleFlash* or *MuzzleSmoke*)
        DOCKING_POINT,   // matches *DockingPoint*
        TRACKS,          // matches *Tracks*
        SMOKE,           // matches *Smoke*
        SPAWN_POINT,     // matches *SpawnPoint*
        EXIT_POINT,      // matches *ExitPoint*
        DOCK_POINT,      // matches *DockPoint*
        CHASSIS,         // matches *Chassis* (first mesh child used for collision baking)
        TURRET_MESH,     // matches *Turret* but not *TurretPivot*
        BARREL_MESH,     // matches *Barrel* but not *BarrelPivot*
        RAMP,            // matches *Ramp*
        WHEEL_FL,        // matches *Wheel_FL*
        WHEEL_FR,        // matches *Wheel_FR*
        WHEEL_RL,        // matches *Wheel_RL*
        WHEEL_RR         // matches *Wheel_RR*
    }
}
```

#### 3.4.3 CollisionBaker.java

```java
/**
 * Stage 3 of the import pipeline.
 * Computes collision bounds from the Chassis mesh vertex data.
 *
 * Two shape types supported:
 *   - RectShape (AABB): full axis-aligned bounding box
 *   - RadialShape (Sphere): minimal bounding sphere derived from AABB
 *
 * Shape selection is driven by the unit's category:
 *   - Soldier (infantry) → RadialShape
 *   - Everything else → RectShape
 */
public class CollisionBaker {

    /**
     * Traverses all Geometry children of chassisNode, collects vertex
     * positions transformed by each geometry's world matrix, and computes
     * the minimal AABB.
     *
     * @param chassisNode  root of the chassis sub-tree
     * @return RectShape with center and half-extents in model-local space
     */
    public RectShape bakeAABB(Node chassisNode);

    /**
     * Derives bounding-sphere radius from an AABB:
     *   radius = max(halfExtents.x, halfExtents.y, halfExtents.z)
     */
    public RadialShape bakeRadius(RectShape aabb);

    /**
     * @return the grid cell footprint as int cell width × depth, rounded up
     *         from the AABB half-extents (for pathfinding grid, used later)
     */
    public int[] bakeGridFootprint(RectShape aabb, float cellSize);
}
```

#### 3.4.4 ModelManifest.java

```java
/**
 * Structured metadata for an imported model.
 * Serialized into the .m3o file header as JSON.
 */
public class ModelManifest {

    private String modelName;
    private String category;           // from UnitConfig.identity.category

    // Detected empty nodes (role → node path in hierarchy)
    private Map<NodeRole, String> roles;    // stores node path strings (serializable)

    // Collision data
    private CollisionShapeData collision;

    // Animation clips (names + params, extracted from skinning data)
    private List<AnimationClipInfo> animations;

    // Bone attach-point metadata
    private List<BoneSlotInfo> boneSlots;

    // --- Inner records for serializable data ---
    public record CollisionShapeData(
        String type,             // "radial" or "rect"
        float[] center,          // [x, y, z]
        float[] halfExtents,     // for rect only (x, y, z)
        float radius             // for radial only
    ) {}

    public record AnimationClipInfo(
        String name,
        float duration,
        boolean looping
    ) {}

    public record BoneSlotInfo(
        String boneName,
        String slotType           // MUZZLE_FLASH, ENGINE_SMOKE, TRACK_DUST
    ) {}
}
```

#### 3.4.5 M3oExporter.java

```java
/**
 * Stage 4 (final) of the import pipeline.
 * Serializes a loaded Node + ModelManifest to a .m3o binary file.
 *
 * .m3o format (Stage 1 version):
 *   Offset | Size  | Field
 *   -------|-------|------------------------------
 *   0      | 4     | Magic: 'M' '3' 'O' 0x00
 *   4      | 4     | Version (uint32 LE): 1
 *   8      | 4     | Manifest JSON byte length (uint32 LE)
 *   12     | N     | Manifest JSON (UTF-8, Gson-serialized)
 *   12+N   | M     | JME BinaryExporter payload (spatial + meshes + materials)
 */
public class M3oExporter {

    /**
     * @param node       JME spatial tree to serialize
     * @param manifest   structured metadata
     * @param outputPath destination .m3o file
     */
    public void exportM3o(Node node, ModelManifest manifest, Path outputPath)
        throws IOException;
}
```

#### 3.4.6 BatchImporter.java

```java
/**
 * CLI entry point for the full build-time import pipeline.
 *
 * Usage:
 *   java -cp build/libs/dune-tools.jar com.jrts.tools.importer.BatchImporter \
 *        --source assets/blender \
 *        --intermediate build/models/intermediate \
 *        --output assets/models/final \
 *        --blender "C:\Program Files\Blender Foundation\Blender 4.0\blender.exe"
 *
 * Workflow per .blend file:
 *   1. Hash .blend → skip if unchanged and .glb exists
 *   2. Invoke Blender CLI → export .glb to intermediate/
 *   3. GltfImporter.importGlb() + validate()
 *   4. EmptyNodeResolver.resolve() → ModelManifest
 *   5. CollisionBaker.bakeAABB() / bakeRadius()
 *   6. Contract validation (mandatory nodes per category)
 *   7. M3oExporter.exportM3o() → final/
 *   8. Print per-file report (OK / WARNINGS / ERRORS)
 *   9. Summary at end: X succeeded, Y warnings, Z errors
 *
 * Exit code 0 = all OK, 1 = warnings (some optional nodes missing), 2 = errors
 */
public class BatchImporter {
    public static void main(String[] args) { /* ... */ }
}
```

#### 3.4.7 Importer Tests

```java
// EmptyNodeResolverTest.java
@Test void resolveTurretPivotExactMatch()       { /* node named "TurretPivot" → TURRET_PIVOT */ }
@Test void resolveCaseInsensitive()              { /* "turretpivot" → TURRET_PIVOT */ }
@Test void resolvePrefixedName()                 { /* "HeavyTank_TurretPivot" → TURRET_PIVOT */ }
@Test void turretMeshNotConfusedWithTurretPivot(){ /* "Turret" ≠ TURRET_PIVOT, "TurretPivot" ≠ TURRET_MESH */ }
@Test void unknownNodeNameReturnsNoRole()         { /* "RandomNode" matches nothing */ }
@Test void multiMatchTakesFirstSpecificPattern()  { /* "TurretPivot" matches TURRET_PIVOT before TURRET_MESH */ }

// CollisionBakerTest.java
@Test void bakeAABBFromSingleBoxMesh() { /* 1x1x1 cube → AABB center(0,0,0), extents(0.5,0.5,0.5) */ }
@Test void bakeAABBFromOffsetMesh()    { /* translated mesh → AABB reflects offset */ }
@Test void bakeRadiusFromAABB()        { /* AABB(extents 2,3,1) → radius 3 */ }
@Test void emptyChassisThrows()        { /* chassis with no geometry → ImportException */ }

// M3oExporterTest.java
@Test void roundtripModelAndManifest() { /* export .m3o → load back → assert manifest equals, spatial structure preserved */ }
@Test void magicBytesAreCorrect()      { /* first 4 bytes = "M3O\0" */ }
```

---

### 3.5 Unit Previewer App

#### 3.5.1 UnitPreviewApp.java

```java
/**
 * Standalone JME application for artist/developer model preview.
 * Launched via: ./gradlew previewModel [path/to/model.m3o]
 *
 * Features:
 *   - Orbit camera (left-drag rotate, scroll zoom, right-drag pan)
 *   - Three-point studio lighting
 *   - Reference floor grid (10×10, 1-unit cells)
 *   - Turret yaw/pitch controls (sliders + real-time rotation of TurretPivot/BarrelPivot)
 *   - Auto-idle-scan toggle
 *   - Muzzle flash particle test (one-shot burst on button press)
 *   - Collision wireframe overlay toggle
 *   - Model info display (vertex count, triangle count, bone count, animation count,
 *     AABB dimensions / radius)
 *   - Animation clip dropdown + play/stop
 *
 * Not a God Class — delegates to:
 *   - OrbitCamera for camera controls
 *   - TurretPreviewController for turret bone manipulation
 *   - CollisionWireframe for debug shape rendering
 *   - ParticlePreviewEmitter for FX testing
 *   - AnimationPreviewController for animation playback
 */
public class UnitPreviewApp extends SimpleApplication {

    // Delegates (injected or created in simpleInitApp)
    private OrbitCamera orbitCamera;
    private TurretPreviewController turretController;
    private String currentModelPath;

    @Override public void simpleInitApp()   { /* setup lighting, grid, load model */ }
    @Override public void simpleUpdate(float tpf) { /* delegate to controllers */ }

    public void loadModel(String m3oPath);   // clears previous, loads new .m3o
    public void unloadModel();               // detach from scene, clear references
}
```

#### 3.5.2 OrbitCamera.java

```java
/**
 * Studio-style orbit camera used ONLY by the preview app.
 * Separate from RtsCamera — different use case (object inspection vs battlefield navigation).
 *
 * Controls:
 *   Left mouse drag → orbit (yaw + pitch)
 *   Scroll wheel    → zoom (distance)
 *   Right mouse drag → pan (move look-at point)
 *
 * Stateless rotation — applies quaternion deltas per frame.
 */
public class OrbitCamera {

    private final Camera cam;
    private final InputManager inputManager;
    private Vector3f lookAt;
    private float distance;
    private float yaw;       // radians
    private float pitch;     // radians

    public void update(float tpf);
    public void setLookAt(Vector3f point);
    public void reset();     // reset to default view (front, 15 units away)
}
```

#### 3.5.3 TurretPreviewController.java

```java
/**
 * Rotates the TurretPivot Node (local Y axis → yaw) and
 * BarrelPivot Node (local X axis → pitch) in response to
 * UI slider values or auto-scan mode.
 *
 * Pure math class — no JME dependency beyond Quaternion/Vector3f.
 */
public class TurretPreviewController {

    private Node turretPivot;
    private Node barrelPivot;

    // Current angle state
    private float targetYawDeg;       // driven by UI slider
    private float targetPitchDeg;     // driven by UI slider
    private float currentYawRad;
    private float currentPitchRad;

    // Auto-scan state
    private boolean autoScan;
    private float scanAngle;
    private float scanDirection = 1f;
    private final float minScanAngle = -45f * FastMath.DEG_TO_RAD;
    private final float maxScanAngle = 45f * FastMath.DEG_TO_RAD;
    private final float scanSpeed = 0.5f;    // rad/s

    /** Apply new yaw target from slider */
    public void setYawDegrees(float degrees);

    /** Apply new pitch target from slider */
    public void setPitchDegrees(float degrees);

    /** Toggle auto-scan (turret sweeps between ±45°) */
    public void setAutoScan(boolean enabled);

    /** Called each frame — applies rotation to actual nodes */
    public void update(float tpf);
}
```

#### 3.5.4 ParticlePreviewEmitter.java and CollisionWireframe.java

Lightweight, single-responsibility classes — see the full plan for signatures.

#### 3.5.5 Previewer Tests

```java
// TurretPreviewControllerTest.java (pure logic, no JME head needed)
@Test void setYawSnapsNodeRotation()    { /* set 90 → turret rotated 90° around Y */ }
@Test void autoScanSweepsWithinLimits() { /* simulate 100 frames → angle stays in [-45,45] */ }

// OrbitCameraTest.java (integration, needs JME head or mock)
@Test void zoomChangesDistance()        { /* ... */ }
```

---

### 3.6 Unit Selection Subsystem

#### 3.6.1 Input Stack for Selection

```
Raw Input → ActionMapper → (two paths)
   ├── left mouse down/move/up → SelectionSystem
   │       ├── click (small drag) → MousePicker.pickObject() → toggle/add unit
   │       └── drag (large drag)  → SelectionBox (rect) → query units in rect → select
   └── right mouse click         → CommandDispatcher
           ├── click terrain → move order for selected units
           └── click unit     → attack-move order (stub in Stage 1)
```

#### 3.6.2 ActionMapper.java

```java
/**
 * Bridges JME RawInputListener events to higher-level named actions.
 * No game logic here — just maps "left mouse press at (x,y)" → action string.
 *
 * Actions defined:
 *   "SELECT_START"   — left mouse down (begin drag or click)
 *   "SELECT_MOVE"    — left mouse drag (update selection rectangle)
 *   "SELECT_END"     — left mouse up (finalize selection)
 *   "ORDER_MOVE"     — right mouse click on terrain
 *   "ORDER_ATTACK"   — right mouse click on enemy unit (stub)
 *   "SHIFT_MOD"      — shift held (modifier for additive selection)
 */
public class ActionMapper implements RawInputListener {

    // Maps mouse buttons, modifier keys to action names
    // Dispatches action + screenX + screenY + modifiers to registered handlers

    public interface InputActionHandler {
        void onAction(String action, float screenX, float screenY, Modifiers mods);
    }

    public void addHandler(InputActionHandler handler);
    public void removeHandler(InputActionHandler handler);
}
```

#### 3.6.3 MousePicker.java

```java
/**
 * Raycasts from screen coordinates against the scene graph.
 *
 * Two modes:
 *   1. pickObject(screenX, screenY) → Optional<Unit>
 *      Ray against all scene geometries, find closest with entityId UserData.
 *
 *   2. pickTerrain(screenX, screenY) → Optional<Vector3f>
 *      Ray against the terrain: uses terrainProvider.getHeight() for Y,
 *      NOT mesh collision. This keeps terrain collision model independent.
 *
 * Uses JME's CollisionResults + BoundingVolume-based broad phase for unit picking.
 */
public class MousePicker {

    private final Node sceneRoot;
    private final UnitRegistry unitRegistry;
    private final Camera cam;
    private final TerrainHeightProvider terrainProvider;

    /**
     * Raycast for the closest selectable unit under the cursor.
     * Filters out spatials without entityId UserData.
     * @return the Unit, or empty if no unit hit
     */
    public Optional<Unit> pickUnit(float screenX, float screenY);

    /**
     * Raycast cursor onto terrain plane.
     * Computes XZ intersection of camera ray with terrain plane,
     * then queries terrainProvider.getHeight() for the Y value.
     *
     * @return world position on terrain surface, or empty if off-map
     */
    public Optional<Vector3f> pickTerrain(float screenX, float screenY) {
        // 1. Compute ray from camera through screen point
        Vector3f origin = cam.getWorldCoordinates(new Vector2f(screenX, screenY), 0f);
        Vector3f direction = cam.getWorldCoordinates(new Vector2f(screenX, screenY), 1f)
            .subtractLocal(origin).normalizeLocal();

        // 2. Intersect with Y=0 plane to find XZ (works because terrain
        //    height variation is small; in Stage 1, Y=0 is the constant height)
        //    For Stage 2+ heightmap terrain, iterate with binary search.
        if (direction.y >= 0) return Optional.empty(); // ray pointing up, no terrain hit
        float t = -origin.y / direction.y;
        float wx = origin.x + t * direction.x;
        float wz = origin.z + t * direction.z;

        // 3. Query terrain height at XZ and return full 3D position
        if (!terrainProvider.isInBounds(wx, wz)) return Optional.empty();
        return Optional.of(new Vector3f(wx, terrainProvider.getHeight(wx, wz), wz));
    }
}
```

#### 3.6.4 SelectionBox.java

```java
/**
 * Tracks a screen-space drag rectangle.
 * Renders a semi-transparent 2D quad.
 *
 * State machine:
 *   IDLE → (mouse down) → DRAGGING → (mouse up) → IDLE
 *     startScreen recorded on down; rect updated each move event.
 *
 * Rendered as a JME Picture or Nifty panel in the GUI viewport
 * (Stage 1 uses a JME Picture overlay).
 */
public class SelectionBox {

    public void start(float screenX, float screenY);   // begin drag
    public void update(float screenX, float screenY);  // resize rectangle
    public void end();                                   // finish drag
    public Rectangle getRect();                          // screen-space rect

    public boolean isActive();
    public boolean isClick();   // true if drag distance < threshold (was a click)
}
```

#### 3.6.5 SelectionSystem.java

```java
/**
 * Manages the current selection set.
 *
 * Single source of truth for "which units are selected".
 * Fires SelectionChangedEvent to observers (UI update, highlight toggle).
 *
 * Inputs:
 *   - click Unit → select single (replace, unless Shift held → toggle)
 *   - drag rectangle → select all selectable units within screen rect
 *   - double-click unit → select all visible units of same type (stub in Stage 1)
 *   - Ctrl+1..9 → assign to control group (stub)
 *   - 1..9 → recall control group (stub)
 */
public class SelectionSystem implements InputActionHandler {

    private final UnitRegistry registry;
    private final MousePicker mousePicker;
    private final SelectionBox selectionBox;
    private final Camera cam;
    private final int screenWidth, screenHeight;

    // Current selection
    private final Set<Unit> selectedUnits = new LinkedHashSet<>();

    // Observers
    private final List<SelectionObserver> observers = new ArrayList<>();

    @Override
    public void onAction(String action, float screenX, float screenY, Modifiers mods) {
        // SELECT_START → selectionBox.start(x, y); if not shift: clearSelection()
        // SELECT_MOVE  → selectionBox.update(x, y)
        // SELECT_END   → if click: pickUnit → select; else: rectangle query → multi-select
    }

    /** Deselect all and notify observers */
    public void clearSelection();

    /** @return unmodifiable view of selected units */
    public Set<Unit> getSelected();

    /** @return true if exactly one unit is selected (for unit info panel) */
    public boolean isSingleSelected();

    /** @return the single selected unit, or empty */
    public Optional<Unit> getSingleSelected();

    public void addObserver(SelectionObserver observer);
    public void removeObserver(SelectionObserver observer);

    /** Observer pattern — HUD and highlight system implement this */
    public interface SelectionObserver {
        void onSelectionChanged(Set<Unit> selected);
    }
}
```

#### 3.6.6 SelectionHighlight.java

```java
/**
 * Renders a circular/rectangular highlight decal under each selected unit.
 *
 * For Stage 1, uses a JME Quad with a circle texture placed at the unit's
 * ground position (Y slightly above terrain to avoid z-fighting).
 *
 * Observes SelectionSystem and toggles visibility per unit.
 */
public class SelectionHighlight implements SelectionSystem.SelectionObserver {

    private final Node sceneRoot;
    private final Map<Unit, Geometry> highlights = new HashMap<>();
    private final Material highlightMaterial;    // semi-transparent green/yellow

    @Override
    public void onSelectionChanged(Set<Unit> selected) {
        // Add highlight quad for newly selected units
        // Remove highlight quad for deselected units
    }
}
```

#### 3.6.7 CommandDispatcher.java

```java
/**
 * Translates right-click actions into orders for the currently selected units.
 *
 * Stage 1 orders:
 *   MOVE:    right-click on terrain → set waypoint (straight line path)
 *   STOP:    (hotkey 'S', stub)
 *
 * Delegates movement to MovementController via each Unit.
 */
public class CommandDispatcher implements InputActionHandler {

    private final SelectionSystem selectionSystem;
    private final MousePicker mousePicker;
    private final NavigationService navigationService;

    @Override
    public void onAction(String action, float screenX, float screenY, Modifiers mods) {
        if ("ORDER_MOVE".equals(action)) {
            Optional<Vector3f> terrain = mousePicker.pickTerrain(screenX, screenY);
            terrain.ifPresent(target -> {
                for (Unit unit : selectionSystem.getSelected()) {
                    if (unit.canMove()) {
                        unit.setWaypoints(navigationService.computePath(
                            unit.position(), target));
                    }
                }
            });
        }
    }
}
```

#### 3.6.8 Selection Tests

```java
// SelectionSystemTest.java
@Test void singleClickSelectsUnit()           { /* pick returns unit → selected set size=1 */ }
@Test void clickDeselectsPrevious()           { /* select A, select B → only B selected */ }
@Test void shiftClickToggles()                { /* select A, shift+click B → {A,B}; shift+click A → {B} */ }
@Test void clickTerrainClearsSelection()      { /* pick returns empty → selected set empty */ }
@Test void dragRectSelectsMultiple()          { /* simulate rect containing 3 units → all 3 selected */ }
@Test void nonSelectableUnitNotSelected()     { /* pick returns unit with SELECTABLE=false → ignored */ }
@Test void observersNotifiedOnChange()        { /* mock observer, verify onSelectionChanged called */ }

// MousePickerTest.java (integration)
@Test void pickUnitHitsClosestSpatial()       { /* two overlapping units → closest returned */ }
@Test void pickTerrainReturnsGroundPosition() { /* known cam + screen pos → expected terrain point */ }

// CommandDispatcherTest.java
@Test void moveOrderSetsWaypointsOnSelected() { /* select tank, right-click terrain → unit.waypoints set */ }
@Test void moveOrderIgnoresNonMovableUnits()  { /* building selected + tank → only tank moves */ }
```

---

### 3.7 Movement Subsystem (Mock Navigation)

#### 3.7.1 NavigationService.java (Interface)

```java
/**
 * Computes a path from start to end position.
 * Implementations:
 *   - SimpleLineNavigation  (Stage 1 mock)
 *   - AStarNavigation       (Stage 2+ real pathfinding)
 */
public interface NavigationService {

    /**
     * @param start  current unit position
     * @param end    desired destination
     * @return ordered list of waypoints (start→...→end).
     *         Always non-null, always at least [end].
     */
    List<Vector3f> computePath(Vector3f start, Vector3f end);
}
```

#### 3.7.2 SimpleLineNavigation.java

```java
/**
 * Stage 1 mock implementation.
 * Returns a single waypoint: the destination itself.
 * No obstacle consideration.
 * Uses TerrainHeightProvider for map-bounds clamping.
 */
public class SimpleLineNavigation implements NavigationService {

    private final TerrainHeightProvider terrain;

    public SimpleLineNavigation(TerrainHeightProvider terrain) {
        this.terrain = terrain;
    }

    @Override
    public List<Vector3f> computePath(Vector3f start, Vector3f end) {
        Vector3f clamped = ScreenMap.clampToMap(end, terrain);
        return Collections.singletonList(clamped);
    }
}
```

#### 3.7.3 MovementController.java

```java
/**
 * Per-frame movement update for ONE unit.
 * Called by the main update loop for each unit with active waypoints.
 *
 * Per-frame logic (from spec Section 3.4):
 *   a. Compute desired velocity toward next waypoint
 *   b. Accelerate/Decelerate toward maxSpeed
 *   c. Turn body toward desired heading (maxTurnRate clamped)
 *   d. If within closeEnoughDist → advance to next waypoint
 *   e. Update world position, clamped to terrain height
 *   f. Set MODELCONDITION_MOVING if speed > 0 (stub in Stage 1)
 *
 * Not a God Class — operates on exactly ONE unit per update call.
 * Depends on TerrainHeightProvider for ground clamping.
 */
public class MovementController {

    private static final float CLOSE_ENOUGH = 0.5f;   // units

    private final TerrainHeightProvider terrain;

    public MovementController(TerrainHeightProvider terrain) {
        this.terrain = terrain;
    }

    /**
     * @param unit     the unit to move
     * @param tpf      time per frame (seconds)
     * @return true if still moving (more waypoints remain), false if arrived
     */
    public boolean update(Unit unit, float tpf) {
        List<Vector3f> waypoints = unit.getWaypoints();
        if (waypoints == null || waypoints.isEmpty()) return false;

        UnitConfig cfg = unit.config();
        float maxSpeed = cfg.stats().speed();
        float turnRate = cfg.stats().rot() * FastMath.DEG_TO_RAD;
        Vector3f target = waypoints.get(0);
        Vector3f pos = unit.position();
        Vector3f toTarget = target.subtract(pos);
        float dist = toTarget.length();

        // Arrived at this waypoint
        if (dist < CLOSE_ENOUGH) {
            waypoints.remove(0);
            return !waypoints.isEmpty();
        }

        // Compute desired heading (in XZ plane — JME Y-up)
        float desiredYaw = (float) Math.atan2(toTarget.x, toTarget.z);

        // Rotate body toward heading
        float yawDiff = normalizeAngle(desiredYaw - unit.bodyYaw());
        float step = Math.min(Math.abs(yawDiff), turnRate * tpf);
        unit.setBodyYaw(unit.bodyYaw() + Math.signum(yawDiff) * step);

        // Move forward (after turning enough)
        if (Math.abs(yawDiff) < 0.1f) {   // roughly facing target
            float speed = Math.min(maxSpeed, dist / tpf);
            Vector3f forward = new Vector3f(
                (float) Math.sin(unit.bodyYaw()),
                0,
                (float) Math.cos(unit.bodyYaw())
            ).multLocal(speed * tpf);
            pos.addLocal(forward);
            // Ground clamp: use terrain height + unit's preferred height
            pos.y = terrain.getHeight(pos.x, pos.z) + unit.getPreferredHeight();
            unit.spatial().setLocalTranslation(pos);
        }

        return true;
    }

    private static float normalizeAngle(float rad) {
        while (rad > FastMath.PI)  rad -= FastMath.TWO_PI;
        while (rad < -FastMath.PI) rad += FastMath.TWO_PI;
        return rad;
    }
}
```

#### 3.7.4 Movement Tests

```java
// SimpleLineNavigationTest.java
@Test void returnsDestinationAsSingleWaypoint() { /* */ }
@Test void clampsDestinationToMapBounds()        { /* dest (200,0,200) with mapSize=100 → clamped */ }

// MovementControllerTest.java
@Test void unitMovesTowardTargetEachFrame() {
    Unit u = createUnitAt(0,0,0, yaw=0);   // facing +Z (north in JME)
    u.setWaypoints(List.of(new Vector3f(0,0,10)));  // 10 units ahead
    boolean moving = controller.update(u, 1.0f/60f);
    assertTrue(moving);
    assertTrue(u.position().z > 0);   // moved forward
}
@Test void unitRotatesBeforeMoving() {
    Unit u = createUnitAt(0,0,0, yaw=0);   // facing +Z
    u.setWaypoints(List.of(new Vector3f(10,0,0)));  // to the right
    // First frame: should rotate, minimal translation
    controller.update(u, 1.0f/60f);
    assertTrue(u.bodyYaw() != 0);            // started turning
    assertTrue(u.position().x < 0.1f);       // barely moved
}
@Test void unitStopsAtCloseEnoughDistance() {
    Unit u = createUnitAt(0,0,0, yaw=0);
    u.setWaypoints(List.of(new Vector3f(0,0,0.3f)));  // within CLOSE_ENOUGH
    boolean moving = controller.update(u, 1.0f/60f);
    assertFalse(moving);                     // waypoint consumed, arrived
    assertTrue(u.getWaypoints().isEmpty());
}
@Test void movesThroughMultipleWaypoints() { /* waypoints=[A,B,C] → consumes A, then B, then C */ }
```

---

### 3.8 Turret Rotation (Following Mouse Cursor)

#### 3.8.1 TurretController.java

```java
/**
 * Rotates a unit's TurretPivot node toward a world-space target point.
 *
 * Stage 1: yaw-only rotation (turret tracks cursor on terrain).
 * Pitch (barrel elevation) deferred to Stage 2+ when weapons are implemented.
 *
 * Called each frame for each selected unit that has a turret.
 * If no explicit target, turret returns to natural angle (idle).
 *
 * Uses the same turn-rate clamping as MovementController.
 */

public class TurretController {

    /**
     * @param unit         unit with turret
     * @param targetPoint  world position to aim at (from mouse-pick on terrain)
     * @param tpf          time per frame
     */
    public void update(Unit unit, Vector3f targetPoint, float tpf) {
        if (!unit.hasTurret() || unit.turretPivot() == null) return;
        Node turretPivot = unit.turretPivot();

        // Compute desired turret yaw (world-space direction from unit to target)
        Vector3f unitPos = unit.position();
        Vector3f toTarget = targetPoint.subtract(unitPos);
        float desiredTurretYaw = (float) Math.atan2(toTarget.x, toTarget.z);

        // Get current turret yaw from pivot's local rotation
        float[] angles = turretPivot.getLocalRotation().toAngles(null);
        float currentYaw = angles[1];  // Y-axis rotation in JME

        float yawDiff = normalizeAngle(desiredTurretYaw - currentYaw);
        float turnRate = unit.config().turrets().turretRotationSpeed() * FastMath.DEG_TO_RAD;
        float step = Math.min(Math.abs(yawDiff), turnRate * tpf);
        float newYaw = currentYaw + Math.signum(yawDiff) * step;

        turretPivot.setLocalRotation(new Quaternion().fromAngleAxis(newYaw, Vector3f.UNIT_Y));
    }

    private static float normalizeAngle(float rad) { /* same as MovementController */ }
}
```

#### 3.8.2 Turret Tests

```java
// TurretControllerTest.java
@Test void turretRotatesTowardTarget()  { /* turret at (0,0,0) facing +Z, target at (10,0,0) → turret rotates right */ }
@Test void turretTracksMovingTarget()    { /* simulate 60 frames, turret gradually catches up to moving cursor */ }
@Test void respectsMaxTurnRate()         { /* huge yaw diff (170°), max step = turnRate * tpf → doesn't snap */ }
@Test void noTurretUnitIsNoOp()          { /* unit without turret → update does nothing, no NPE */ }
```

---

### 3.9 Scene System (Terrain Provider)

Defines how all systems query terrain height. The interface is designed so Stage 1 returns constant 0, and Stage 2+ swaps in a heightmap-backed implementation without changing any caller.

#### 3.9.1 TerrainHeightProvider.java (Interface)

```java
/**
 * Single interface for all terrain height queries.
 * All game systems depend on this interface, never on concrete terrain classes.
 * Thread-safe by contract (designed for future worker-thread pathfinding).
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

#### 3.9.2 FlatTerrainHeightProvider.java

```java
/**
 * Stage 1 implementation — returns constant height for every point.
 * When Stage 2+ introduces PNG heightmaps, swap this class with
 * HeightmapTerrainProvider. No other system code changes.
 *
 * Map bounds are set at construction time.
 */
public class FlatTerrainHeightProvider implements TerrainHeightProvider {

    private final float height;     // constant Y, default 0
    private final float mapSize;    // square map half-width

    public FlatTerrainHeightProvider(float height, float mapSize) {
        this.height = height;
        this.mapSize = mapSize;
    }

    @Override
    public float getHeight(float worldX, float worldZ) { return height; }

    @Override
    public boolean isWater(float worldX, float worldZ) { return false; }

    @Override
    public float getGradient(float worldX, float worldZ) { return 0f; }

    @Override
    public boolean isInBounds(float worldX, float worldZ) {
        return Math.abs(worldX) <= mapSize && Math.abs(worldZ) <= mapSize;
    }

    @Override
    public float getMapMinX() { return -mapSize; }
    @Override
    public float getMapMaxX() { return mapSize; }
    @Override
    public float getMapMinZ() { return -mapSize; }
    @Override
    public float getMapMaxZ() { return mapSize; }
    @Override
    public float getWaterLevel() { return -Float.MAX_VALUE; } // never water
}
```

#### 3.9.3 TerrainPlane.java

```java
/**
 * Visual terrain representation for Stage 1.
 * A single flat quad with tan/sand material.
 * Height data is provided by TerrainHeightProvider (constant 0 in Stage 1).
 *
 * Does NOT implement collision — all collision goes through
 * TerrainHeightProvider.getHeight() instead.
 */
public class TerrainPlane {

    private final Geometry geometry;

    public TerrainPlane(TerrainHeightProvider terrain, float width, float depth,
                        AssetManager assetManager) {
        // Create a flat quad mesh (2 triangles)
        // Position it at (0, terrain.getHeight(0,0), 0)
        // Apply tan/sand colored material
    }

    public Geometry getGeometry() { return geometry; }
}
```

#### 3.9.4 ScreenMap Fix References

`ScreenMap.screenToTerrain()` now takes `TerrainHeightProvider`:

```java
public static Vector3f screenToTerrain(
    float screenX, float screenY, Camera cam, TerrainHeightProvider terrain
) {
    // Compute ray from camera through screen point
    // Intersect with Y=0 plane to find XZ
    // Use terrain.getHeight(wx, wz) for final Y
    // (binary search iteration if ray-plane intersection is inaccurate for sloped terrain)
}
```

---

### 3.10 Scene Bootstrapper

#### 3.10.1 SceneBootstrapper.java

```java
/**
 * Sets up the Stage 1 scene: terrain, lighting, sky, and initial units.
 *
 * Called once at app startup (equivalent to simpleInitApp in JME terms).
 * Delegates to focused helper methods rather than doing everything inline.
 *
 * Creates a FlatTerrainHeightProvider (constant height Y=0) for Stage 1.
 * When real terrain is ready in Stage 2+, swap to HeightmapTerrainProvider
 * — the interface is identical.
 */
public class SceneBootstrapper {

    private final Node rootNode;
    private final AssetManager assetManager;
    private final Camera cam;
    private final ConfigLoader configLoader;
    private final ModelLoader modelLoader;
    private final UnitFactory unitFactory;

    // Stage 1: flat terrain. Will be replaced by HeightmapTerrainProvider.
    private final FlatTerrainHeightProvider terrainProvider;

    public SceneBootstrapper(
            Node rootNode, AssetManager assetManager, Camera cam,
            ConfigLoader configLoader, ModelLoader modelLoader,
            UnitFactory unitFactory, float mapSize) {
        this.rootNode = rootNode;
        this.assetManager = assetManager;
        this.cam = cam;
        this.configLoader = configLoader;
        this.modelLoader = modelLoader;
        this.unitFactory = unitFactory;
        this.terrainProvider = new FlatTerrainHeightProvider(0f, mapSize);
    }

    /** @return the terrain provider (needed by other systems) */
    public TerrainHeightProvider getTerrainProvider() {
        return terrainProvider;
    }

    /**
     * Full bootstrap: creates terrain, lights, then loads and spawns units.
     */
    public void bootstrap() {
        createTerrain();
        createLighting();
        createSky();
        spawnInitialUnits();
    }

    private void createTerrain() {
        // 100×100 unit flat quad, centered at origin
        // Uses FlatTerrainHeightProvider for height (constant 0)
        // Material: simple tan/sand color, tiling texture
        // Stored as userData "terrain" for picker raycast (optional)
        TerrainPlane plane = new TerrainPlane(
            terrainProvider, 100f, 100f, assetManager);
        rootNode.attachChild(plane.getGeometry());
    }

    private void createLighting() {
        // DirectionalLight from upper-left (simulates sun)
        // AmbientLight at 30% to fill shadows
    }

    private void createSky() {
        // viewPort.setBackgroundColor(new ColorRGBA(0.5f, 0.6f, 0.7f, 1.0f));
    }

    private void spawnInitialUnits() {
        UnitConfig tankConfig = configLoader.loadUnitConfig("heavy_tank");
        LoadedModel tankModel = modelLoader.loadM3o("models/final/heavy_tank.m3o");
        // Snap to terrain height at spawn position
        Vector3f spawnPos = new Vector3f(0, 0, 0);
        spawnPos.y = terrainProvider.getHeight(spawnPos.x, spawnPos.z);
        unitFactory.create(tankConfig, tankModel, spawnPos, 0.5f);
    }
}
```

---

### 3.11 Main.java (Application Entry Point)

```java
/**
 * Main entry point. Extends JME's SimpleApplication.
 *
 * Orchestrates initialization, wires all subsystems together,
 * and runs the main update loop.
 *
 * This is the ONLY class that "knows about everything" — it acts as
 * the composition root (DI container). It does NOT contain business logic.
 */
public class Main extends SimpleApplication {

    // Subsystems (set up in simpleInitApp, wired here)
    private RtsCamera rtsCamera;
    private RtsCameraInputListener cameraInput;
    private ScreenMap screenMap;
    private ActionMapper actionMapper;
    private MousePicker mousePicker;
    private SelectionBox selectionBox;
    private SelectionSystem selectionSystem;
    private SelectionHighlight selectionHighlight;
    private CommandDispatcher commandDispatcher;
    private MovementController movementController;
    private TurretController turretController;
    private NavigationService navigationService;
    private TerrainHeightProvider terrainProvider;
    private UnitFactory unitFactory;
    private UnitRegistry unitRegistry;
    private SceneBootstrapper sceneBootstrapper;

    public static void main(String[] args) {
        Main app = new Main();
        app.start();  // JME app lifecycle
    }

    @Override
    public void simpleInitApp() {
        // 1. Disable default flyCam (we use RtsCamera)
        flyCam.setEnabled(false);

        // 2. Create core data registries
        unitRegistry = new UnitRegistry();
        UnitFactory unitFactory = new UnitFactory(unitRegistry);

        // 3. Create terrain provider (flat constant Y=0 for Stage 1)
        float mapSize = 100f;
        terrainProvider = new FlatTerrainHeightProvider(0f, mapSize);

        // 4. Create movement & navigation
        movementController = new MovementController(terrainProvider);
        navigationService = new SimpleLineNavigation(terrainProvider);

        // 5. Create camera
        rtsCamera = new RtsCamera(cam);
        rtsCamera.update(0f);

        // 6. Create input and selection systems
        mousePicker = new MousePicker(rootNode, unitRegistry, cam, terrainProvider);
        selectionBox = new SelectionBox(assetManager);
        selectionSystem = new SelectionSystem(unitRegistry, mousePicker, selectionBox, cam,
            settings.getWidth(), settings.getHeight());
        selectionHighlight = new SelectionHighlight(rootNode, assetManager);
        selectionSystem.addObserver(selectionHighlight);

        // 7. Create command dispatcher
        commandDispatcher = new CommandDispatcher(selectionSystem, mousePicker, navigationService);

        // 8. Create turret controller
        turretController = new TurretController();

        // 9. Register input listeners
        cameraInput = new RtsCameraInputListener(rtsCamera, settings.getWidth(), settings.getHeight());
        inputManager.addRawInputListener(new ActionMapper(
            selectionSystem, commandDispatcher, cameraInput));

        // 10. Bootstrap scene (terrain + initial units)
        sceneBootstrapper = new SceneBootstrapper(rootNode, assetManager, cam,
            configLoader, modelLoader, unitFactory, mapSize);
        sceneBootstrapper.bootstrap();
    }

    @Override
    public void simpleUpdate(float tpf) {
        // 1. Camera
        rtsCamera.update(tpf);

        // 2. For each selected unit with turret: track cursor on terrain
        Vector3f cursorWorldPos = mousePicker.pickTerrain(
            inputManager.getCursorPosition().x,
            inputManager.getCursorPosition().y
        ).orElse(null);

        for (Unit unit : selectionSystem.getSelected()) {
            if (unit.hasTurret() && cursorWorldPos != null) {
                turretController.update(unit, cursorWorldPos, tpf);
            }

            // 3. Movement
            if (unit.canMove() && unit.getWaypoints() != null
                    && !unit.getWaypoints().isEmpty()) {
                movementController.update(unit, tpf);
            }
        }

        // 4. Scene graph auto-rendered by JME
    }
}
```

---

## 4. Thread Model (Stage 1)

Stage 1 uses a **single-threaded** update loop. JME runs `simpleUpdate` on the main (render) thread.

- All subsystems execute sequentially each frame
- No concurrency concerns — simple, predictable, easy to test
- Pathfinding (when real A* arrives in Stage 2+) will move to a worker thread with `CompletableFuture`

---

## 5. Dependency Graph (Constructor Injection)

```
Main (composition root)
 ├── TerrainHeightProvider (FlatTerrainHeightProvider, shared instance)
 ├── RtsCamera (Camera)
 ├── RtsCameraInputListener (RtsCamera, screenW, screenH)
 ├── ScreenMap (static utils, takes terrain as param)
 ├── ActionMapper (InputManager)
 │    ├── SelectionSystem (UnitRegistry, MousePicker, SelectionBox, Camera)
 │    │    ├── MousePicker (Node, UnitRegistry, Camera, TerrainHeightProvider)
 │    │    ├── SelectionBox (AssetManager for material)
 │    │    └── SelectionHighlight (Node, observes SelectionSystem)
 │    └── CommandDispatcher (SelectionSystem, MousePicker, NavigationService)
 ├── MovementController (TerrainHeightProvider — pure logic)
 ├── TurretController (no deps; operates on Unit's Node refs)
 ├── NavigationService (SimpleLineNavigation, takes TerrainHeightProvider)
 ├── ConfigLoader (Path to config dir)
 ├── ModelLoader (AssetManager)
 ├── UnitFactory (UnitRegistry, AtomicInteger)
 ├── UnitRegistry (HashMaps)
 └── SceneBootstrapper (Node, AssetManager, ConfigLoader, ModelLoader, UnitFactory, TerrainHeightProvider)
```

All dependencies are **interfaces where possible**, **concrete classes where pragmatism wins** (Stage 1 deliberately keeps things simple).

| Interface | Concrete (Stage 1) | Replaced In |
|---|---|---|
| `NavigationService` | `SimpleLineNavigation` | Stage 2 (`AStarNavigation`) |
| `TerrainHeightProvider` | `FlatTerrainHeightProvider` | Stage 2 (`HeightmapTerrainProvider`) |
| (none yet) | `ConfigLoader` reads TOML | Stage 3+ (caching layer could wrap it) |
| (none yet) | `ModelLoader` reads .m3o | Stage 3+ (async preloader wrapper) |

---

## 6. Test Strategy Summary

| Category | Framework | Approach |
|---|---|---|
| **Pure logic** (MovementController, TurretController, UnitFlags, CollisionBaker math) | JUnit 5 | Unit tests, no JME head needed, fast |
| **Config parsing** (ConfigLoader, TOML → records) | JUnit 5 | Load real .toml files from test resources; verify every field |
| **Importer pipeline** (EmptyNodeResolver, M3oExporter roundtrip) | JUnit 5 | Golden .glb test fixtures in `src/test/resources/`; assert manifests |
| **JME-integrated** (MousePicker, SelectionSystem, RtsCamera) | JUnit 5 + JME headless | `HeadlessApplication` or mock Camera; verify Spatial interactions; slower but essential |
| **Input mapping** (ActionMapper) | JUnit 5 + Mockito | Mock InputManager; verify correct action names dispatched |
| **End-to-end scene** (full app) | Manual + JME headless smoke test | Launch app, verify scene renders without crashes; move unit; select; rotate turret |

### Test File Layout

```
src/test/java/com/dunerpg/
├── camera/
│   ├── RtsCameraTest.java
│   └── ScreenMapTest.java
├── config/
│   └── ConfigLoaderTest.java
├── unit/
│   ├── UnitFlagsTest.java
│   ├── UnitFactoryTest.java
│   └── UnitRegistryTest.java
├── movement/
│   ├── SimpleLineNavigationTest.java
│   └── MovementControllerTest.java
├── turret/
│   └── TurretControllerTest.java
├── terrain/
│   ├── FlatTerrainHeightProviderTest.java
│   └── TerrainHeightProviderTest.java
├── input/
│   ├── ActionMapperTest.java
│   ├── SelectionSystemTest.java
│   ├── MousePickerTest.java
│   └── CommandDispatcherTest.java
├── tools/importer/
│   ├── EmptyNodeResolverTest.java
│   ├── CollisionBakerTest.java
│   └── M3oExporterTest.java
└── tools/preview/
    └── TurretPreviewControllerTest.java

src/test/resources/
├── config/
│   ├── heavy_tank_example.toml    (copy of actual config)
│   ├── weapon_example.toml
│   └── building_example.toml
└── models/
    └── test_chassis.glb            (minimal glTF: 1×1×1 cube named "Chassis" + TurretPivot empty)
```

---

## 7. Build & Run

### 7.1 Gradle Tasks

```bash
# Run full game (Stage 1 demo scene)
./gradlew run

# Run unit previewer (load a specific .m3o)
./gradlew previewModel --args="assets/models/final/heavy_tank.m3o"

# Import all .blend → .m3o (requires Blender installed)
./gradlew importModels

# Run all tests
./gradlew test

# Run only fast unit tests (exclude JME-headless integration tests)
./gradlew test -PfastOnly
```

### 7.2 Controls (Stage 1)

| Input | Action |
|---|---|
| W/A/S/D or Arrow keys | Pan camera |
| Mouse at screen edge | Auto-scroll camera |
| Right mouse drag | Drag-scroll (reverse drag) |
| Middle mouse drag | Rotate camera yaw |
| Scroll wheel | Zoom in/out |
| Left click on unit | Select unit |
| Shift + left click | Toggle unit selection |
| Left drag on terrain | Rectangle-select units |
| Left click on terrain | Deselect all |
| Right click on terrain | Move selected units (straight-line path) |
| Mouse cursor (turret tracks) | Selected tank's turret rotates toward cursor-ground-intersection |

---

## 8. Stage 1 Completion Checklist

- [ ] `RtsCamera` pans, zooms, and rotates smoothly
- [ ] `ScreenMap` correctly converts screen ↔ terrain ↔ world coordinates
- [ ] `ConfigLoader` parses all 3 .toml files without errors, all fields populated
- [ ] `UnitConfig`, `WeaponConfig`, `BuildingConfig` are immutable records with typed fields
- [ ] `GltfImporter` loads a .glb and produces a valid JME Node tree
- [ ] `EmptyNodeResolver` correctly classifies TurretPivot, BarrelPivot, Chassis, Muzzle, etc.
- [ ] `CollisionBaker` produces correct AABB and radius from a known mesh
- [ ] `M3oExporter` round-trips: export .m3o → reload → same spatial + same manifest
- [ ] `BatchImporter` CLI runs without errors on a directory of .blend files
- [ ] `FlatTerrainHeightProvider` returns constant height everywhere, never water, gradient 0
- [ ] `ScreenMap.screenToTerrain()` queries terrain provider for correct Y after ray-plane XZ
- [ ] Muzzle flash particle emits from the previewer's "Fire" button
- [ ] Collision wireframe overlay toggles on/off in previewer
- [ ] `UnitRegistry` correctly registers, finds by ID and Spatial, and unregisters
- [ ] `UnitFactory` creates a Unit with correct flags, position, and yaw
- [ ] Left-click on unit selects it; left-click on terrain deselects
- [ ] Shift+click toggles selection; drag-rectangle selects multiple units
- [ ] `SelectionHighlight` shows a green decal under selected units
- [ ] Right-click on terrain issues move order; unit travels straight-line path
- [ ] `MovementController` accelerates, turns body toward heading, decelerates on arrival
- [ ] Tank turret smoothly tracks mouse cursor on terrain (yaw-only)
- [ ] All pure-logic tests pass (MovementController, TurretController, UnitFlags, CollisionBaker, etc.)
- [ ] All config parsing tests pass with real .toml files
- [ ] Importer pipeline tests pass (EmptyNodeResolver, M3oExporter roundtrip)
- [ ] No God Classes — every class ≤ 500 lines, single responsibility verifiable from class name
- [ ] All classes have Javadoc on public methods and class-level responsibility statement
- [ ] All classes under `tools/` have `@author` and `@since Stage-1` Javadoc tags
- [ ] Flat 100×100 terrain renders with tan/sand material
- [ ] One HeavyTank unit is visible, selectable, movable, with rotating turret
- [ ] Application exits cleanly with `app.stop()` (no leaked JME/GL resources)
