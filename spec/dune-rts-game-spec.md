# 3D RTS Game Specification — Key Techniques Derived from C&C Generals / Dune

## 1. Overview

This document specifies key game systems for a Dune-themed 3D RTS game, derived from the Command & Conquer: Generals Zero Hour open-source release. Only foundational RTS techniques are included. Advanced mechanics (hacking, jump-jets, rappel, combat drop, battle plans, fanaticism, mine clearing, etc.) are deliberately excluded.

### Design Goals
- **Flat/slightly-varied terrain** — no bridges, walls, or cliffs at launch; simplifies collision and pathfinding
- **Simple collision** — radial (circle/sphere) and rectangular (AABB) shapes; collision checked via simple formulas, no mesh-based collision
- **Standard RTS controls** — mouse selection, rectangular drag-select, right-click orders
- **MODELCONDITION-driven animation** — runtime state maps to bone-level animation conditions
- **Data-driven design** — all unit/weapon properties loaded from config files (INI-style)

---

## 2. Unit System

### 2.1 Unit Lifecycle

```
ThingTemplate (JSON/YAML data)
  └── Object (runtime instance)
        ├── BodyModule        — health, armor, damage handling
        ├── AIUpdateInterface — movement, state machine, pathfinding
        ├── CollideModule     — collision response
        ├── Drawable          — visual representation (mesh + animation)
        └── Physics (optional)— rigid body for death/destruction
```

### 2.2 Unit Capability Flags

Each unit template defines Boolean flags controlling core behaviors:

| Flag | Description |
|---|---|
| `CanMove` | Unit can receive move orders and pathfind. False = immobile (buildings, deployed units). |
| `CanBeSelected` | Unit appears in selection rectangles and can be clicked. False = projectiles, invisible scouts. |
| `CanBePicked` | Unit can be individually selected/queried by the player. False = non-interactive decorators. |
| `IsStructure` | Unit is a building (snaps to grid, blocks pathfinding, has build animation). |
| `IsProjectile` | Unit is a weapon projectile (fast, ignores physics, collides per weapon mask). |
| `Selectable` | Unit responds to group selection. False = cinematic-only units. |

These flags exist as bits in a `UnitFlags` bitmask:

```cpp
enum UnitFlags : uint32_t
{
    FLAG_CAN_MOVE         = 0x0001,
    FLAG_CAN_BE_SELECTED  = 0x0002,
    FLAG_CAN_BE_PICKED    = 0x0004,
    FLAG_IS_STRUCTURE     = 0x0008,
    FLAG_IS_PROJECTILE    = 0x0010,
    FLAG_SELECTABLE       = 0x0020,
    FLAG_AIRBORNE         = 0x0100,
    FLAG_DOCKABLE         = 0x0200,
    FLAG_CAN_BUILD        = 0x0400,
    FLAG_CAN_GATHER       = 0x0800,
    FLAG_HAS_TURRET       = 0x1000,
};
```

### 2.3 MODELCONDITION Animation States

Animation blending is driven by `MODELCONDITION` flags (mapped from C&C Generals). Each condition triggers a bone-level animation graph node:

| Condition | Trigger |
|---|---|
| `MODELCONDITION_MOVING` | Unit is pathfinding / moving |
| `MODELCONDITION_TURRET_ROTATE` | Turret is rotating toward target |
| `MODELCONDITION_FIRING_A` / `_B` | Weapon slot firing |
| `MODELCONDITION_PREATTACK_A` | Weapon slot pre-attack delay |
| `MODELCONDITION_RELOADING_A` | Weapon slot reloading |
| `MODELCONDITION_DYING` | Death sequence |
| `MODELCONDITION_DOCKING` | Unit entering/leaving dock |
| `MODELCONDITION_BUILDING` | Structure under construction |
| `MODELCONDITION_DEPLOYED` | Unit in deployed state |
| `MODELCONDITION_GARRISONED` | Unit inside a garrisonable structure |
| `MODELCONDITION_AFLAME` | Unit is burning (damaged) |
| `MODELCONDITION_FREEFALL` | Unit falling (destruction physics) |

---

## 3. Locomotor System (Movement)

### 3.1 Architecture

```
LocomotorTemplate (data, loaded from config)
  └── Locomotor (runtime instance, created via LocomotorStore)
        └── LocomotorSet (collection of locomotor types per unit)
```

### 3.2 Basic Locomotor Types

Only four fundamental types are supported:

| Type | Internal Name | Surface | Description |
|---|---|---|---|
| Infantry | `LOCO_LEGS_TWO` | Ground | Bipedal, slow, tight turning |
| Wheeled | `LOCO_WHEELS_FOUR` | Ground | Fast on roads, medium turning |
| Tracked | `LOCO_TREADS` | Ground/Rough | All-terrain, slow, wide turning |
| Hover | `LOCO_HOVER` | Ground/Water | Floats above surface, smooth movement |

(`LOCO_THRUST`, `LOCO_WINGS`, `LOCO_CLIMBER`, `LOCO_MOTORCYCLE`, downhill-only, and other specialized types are excluded.)

### 3.3 Locomotor Parameters

| Parameter | Type | Description |
|---|---|---|
| `maxSpeed` | float (units/s) | Top movement speed |
| `maxSpeedDamaged` | float | Speed when severely damaged |
| `acceleration` | float (units/s²) | Rate of speed increase |
| `deceleration` | float (units/s²) | Braking rate |
| `maxTurnRate` | float (rad/s) | Maximum turning speed |
| `minTurnSpeed` | float | Minimum speed before turning is allowed |
| `closeEnoughDist` | float | Distance threshold to consider target reached |
| `surfaces` | bitmask | Surface types allowed (ground=1, water=2) |
| `preferredHeight` | float | Altitude above terrain (0 = ground, >0 = hover/flight) |

### 3.4 Movement Per-Frame

```
1. PathfindResult → generates waypoint list
2. For each frame:
   a. Compute desired velocity toward next waypoint
   b. Accelerate/Decelerate toward maxSpeed
   c. Turn body toward desired heading (maxTurnRate clamped)
   d. If within closeEnoughDist → advance to next waypoint
   e. Update world position
   f. Set MODELCONDITION_MOVING if speed > 0
```

---

## 4. Pathfinding (Simplified)

### 4.1 Grid-Based A*

The map is divided into a uniform grid (default cell size: 10 world units). A standard A* algorithm finds paths:

```
Grid:        MapWidth/CellSize × MapHeight/CellSize cells
Cell Types:  CLEAR (walkable), BLOCKED (structures/water), SOFT_BLOCKED (units present)
Heuristic:   Octile distance (accounts for diagonal movement)
```

### 4.2 Cell Flags

| Flag | Value | Description |
|---|---|---|
| `CELL_CLEAR` | 0 | Walkable terrain |
| `CELL_BLOCKED` | 1 | Impassable (structures, water for non-aquatic) |
| `CELL_SOFT_BLOCKED` | 2 | Occupied by another unit (repulsion possible) |

### 4.3 Pathfinding Flow

```
1. Unit requests path (start → goal)
2. If start/goal cells BLOCKED → fail
3. A* open/closed lists until goal reached or exhausted
4. Path optimization: remove co-linear intermediate nodes
5. Unit follows waypoint chain via locomotor
6. If blocked by another unit → attempt repulsion; if stuck > N frames → re-path
```

### 4.4 Area-Based Simplification

Instead of full zone/layer/bridge systems:
- No bridges at launch — all terrain is flat ground with obstacles
- No multi-layer pathfinding — one layer: ground
- Pathfinding queries use a bounding-sphere margin to avoid obstacle corners

---

## 5. Collision System (Simplified)

### 5.1 Collision Shapes

Only two primitive shapes are used. No mesh-based collision.

| Shape | Definition | Use Case |
|---|---|---|
| **Radial** (Circle/Sphere) | Center + Radius | Infantry, projectiles, small vehicles |
| **Rectangular** (AABB) | Center + Extent (half-widths) | Buildings, tanks, large units |

### 5.2 Collision Math

Given two objects A and B:

**Radial vs. Radial:**
```
distance = |centerB - centerA|
collision = distance < (radiusA + radiusB)
```

**Radial vs. Rectangular (AABB vs. Circle):**
```
closestX = clamp(circle.center.x, box.minX, box.maxX)
closestY = clamp(circle.center.y, box.minY, box.maxY)
distSqr = sqr(circle.center.x - closestX) + sqr(circle.center.y - closestY)
collision = distSqr < sqr(circle.radius)
```

**Rectangular vs. Rectangular (AABB vs. AABB, any rotation allowed via OBBox):**
```
// Separating Axis Theorem: project both boxes onto each axis.
// If any axis has non-overlapping projections → no collision.
for each axis (2 from BoxA + 2 from BoxB):
    // project both boxes
    // if maxA < minB or maxB < minA → separate
collision = all 4 axes overlap
```

**Ground Collision:**
```
// Unit Z (height) is clamped to terrain height at unit's XY position
// Buildings are axis-aligned, so ground collision is trivial
object.z = terrainHeight(object.x, object.y) + object.preferredHeight
```

### 5.3 Collision Events

```
Object::collideWith(other):
    // Determine by shape pair (radial, rect, radial-rect, rect-radial)
    if (collisionDetected):
        notify this.onCollide(other, contactPoint, normal)
        notify other.onCollide(this, contactPoint, -normal)
```

The `onCollide()` interface:
- Projectiles: detonate, deal damage, destroy self
- Units: trigger repulsion force away from each other
- Docking units: trigger dock/undock entry

---

## 6. Weapon System

### 6.1 Architecture

```
WeaponTemplate (data, loaded from config)
  └── Weapon (runtime instance, per slot)
        └── WeaponSet (per-unit collection of up to 3 weapons)
```

### 6.2 Weapon Slots

Each unit has up to 3 weapon slots:

| Slot | Name | Usage |
|---|---|---|
| 0 | `PRIMARY` | Main weapon (rifle, cannon, missile) |
| 1 | `SECONDARY` | Alternate weapon (rockets, special) |
| 2 | `TERTIARY` | Rare third weapon |

### 6.3 Key Weapon Types

The spec covers these fundamental weapon archetypes:

| Type | Description | Projectile | Damage Model |
|---|---|---|---|
| **Instant/Hitscan** | No travel time, damage applied immediately | No | Point damage |
| **Ballistic** | Fast-moving projectile follows straight line | Yes (speed > 0) | Point + splash |
| **Lobber** | Slow, arcing projectile (Bezier curve) | Yes | Splash-centered |
| **Contact** | Weapon fires when owner collides with target | No | Point/AoE at self |

### 6.4 WeaponTemplate Parameters

| Parameter | Type | Description |
|---|---|---|
| `name` | string | Weapon identifier |
| `primaryDamage` | float | Base damage per shot |
| `primaryDamageRadius` | float | Splash radius (0 = point damage) |
| `secondaryDamage` | float | Outer ring damage |
| `secondaryDamageRadius` | float | Outer ring radius (> primaryRadius) |
| `attackRange` | float | Maximum firing distance |
| `minimumAttackRange` | float | Minimum distance (cannot fire if too close) |
| `weaponSpeed` | float | Projectile travel speed (0 = instant) |
| `damageType` | enum | ARMOR_PIERCING, EXPLOSION, FLAME, POISON, etc. |
| `deathType` | enum | NORMAL, EXPLODED, BURNED, etc. (triggers death animation) |
| `clipSize` | int | Shots before reload (0 = infinite) |
| `clipReloadTime` | int | Frames to reload empty clip |
| `delayBetweenShots` | int | Frames between successive shots |
| `shotsPerBarrel` | int | Shots from each fire-point before switching |
| `preAttackDelay` | int | Charge-up delay before firing |
| `reloadType` | enum | AUTO_RELOAD, NO_RELOAD, RETURN_TO_BASE |
| `prefireType` | enum | PER_SHOT, PER_ATTACK, PER_CLIP |
| `collideMask` | bitmask | What non-target objects this projectile can hit |
| `projectileTemplate` | string | Projectile object to spawn (if any) |
| `fireFX` | FXList* | Effects to play at muzzle |
| `detonateFX` | FXList* | Effects to play at impact |

### 6.5 Basic Damage Types, later more could be added

| Type               | Effective Against         | Ineffective Against | Visual Effect     |     |
| ------------------ | ------------------------- | ------------------- | ----------------- | --- |
| **Kinetic (AP)**   | Heavy armor               | Light armor         | Small white hit   |     |
| **HE (Explosive)** | Buildings, Light armor    | Heavy armor         | Fireball          |     |
| **Laser**          | All armor (50% reduction) | N/A                 | Beam flash        |     |
| **Flame**          | Infantry, Light structure | Vehicles            | Expanding fire    |     |

### 6.6 Damage Calculation Formula


```
base_damage = weapon.damage
armor_multiplier = armor_table[damage_type][target_armor_type]
range_multiplier = 1.0 - (distance / weapon.range) * 0.3  # falloff at max range
if weapon.accuracy_roll():
    final_damage = base_damage * armor_multiplier * range_multiplier
else:
    final_damage = base_damage * 0.5  # glancing blow
    apply_suppression(target)  # reduces accuracy temporarily
```

### 6.7 Collision Mask

```cpp
enum CollideMask : uint32_t
{
    COLLIDE_ALLIES     = 0x01,
    COLLIDE_ENEMIES    = 0x02,
    COLLIDE_STRUCTURES = 0x04,
    COLLIDE_WALLS      = 0x08,
    COLLIDE_VEHICLES   = 0x10,
    COLLIDE_INFANTRY   = 0x20,
};
```

The target is **always** collided with. The mask filters incidental hits.

### 6.8 Weapon Status Machine

```
                  ┌──────────────────────────────────────────┐
                  │                                          │
                  ▼                                          │
READY_TO_FIRE ──▶ PRE_ATTACK (delay) ──▶ fire shot          │
     ▲                                        │              │
     │                      ┌─────────────────┘              │
     │                      ▼                                │
     │               ammo left?                              │
     │               YES → BETWEEN_FIRING_SHOTS (delay) ─────┘
     │               NO  → OUT_OF_AMMO
     │                      │
     │                      ▼ auto-reload?
     │                      YES → RELOADING_CLIP (delay) ────┘
     │                      NO  → OUT_OF_AMMO (permanent)
     └──────────────────── (reload complete)
```

### 6.9 WeaponSet Selection Logic

```
chooseBestWeaponForTarget(target):
    1. Filter weapons: isWithinAttackRange(source, target) && not isTooClose
    2. Filter by anti-mask: can this weapon type target this unit type?
    3. Prefer weapon with highest estimatedDamage(target)
    4. If tie: prefer PRIMARY > SECONDARY > TERTIARY
```

---

## 7. Turret System

### 7.1 Architecture

Derived from `TurretAI.cpp` (`TurretAI.h`). Each unit with turrets has:

```
AIUpdateInterface
  ├── TurretAI[0] (main turret)
  └── TurretAI[1] (alt turret, optional)
        └── TurretStateMachine (finite state machine)
```

### 7.2 Turret Data Parameters

| Parameter | Type | Description |
|---|---|---|
| `turnRate` | float (rad/s) | Turret rotation speed |
| `pitchRate` | float (rad/s) | Turret pitch speed (if allowsPitch) |
| `naturalTurretAngle` | float | Resting/neutral turret angle (relative to body) |
| `naturalTurretPitch` | float | Resting/neutral turret pitch |
| `firePitch` | float | Fixed fire pitch (0 = aim at target directly) |
| `minPitch` | float | Minimum pitch limit (cannot aim below this) |
| `allowsPitch` | bool | True = turret can tilt up/down |
| `controlledWeaponSlots` | bitmask | Which weapon slots this turret controls |
| `fireAngleSweep[slot]` | float | Sweep angle for suppression/scatter fire |
| `firesWhileTurning` | bool | Can shoot before fully aimed |
| `minIdleScanAngle` | float | Min idle scan offset from natural angle |
| `maxIdleScanAngle` | float | Max idle scan offset from natural angle |
| `idleScanInterval` | int (frames) | Time between idle scan movements |
| `recenterTime` | int (frames) | Time to hold position before returning to natural |

### 7.3 Turret State Machine

```
TURRETAI_IDLE ──scan_timeout──▶ TURRETAI_IDLESCAN
     ▲                              │ scan_complete
     │                              ▼
     │                        (back to IDLE)
     │
     ├── target_set ──▶ TURRETAI_AIM
     │                       │ aimed && in_range
     │                       ▼
     │                  TURRETAI_FIRE (weapon fires)
     │                       │ shot complete
     │                       ▼
     │                  (back to AIM if target alive, else HOLD)
     │
     ▼
TURRETAI_HOLD ──timeout──▶ TURRETAI_RECENTER ──centered──▶ IDLE
     ▲
     └── target_lost
```

### 7.4 Turret Aim Logic (per frame)

```
TurretAIAimTurretState::update():
    1. Validate target (not dead, still attackable, in same team)
    2. Compute relative angle to target (ThePartitionManager::getRelativeAngle2D)
    3. If sweep enabled: add ±sweepAngle to aimAngle (waggles spread)
    4. Turn turret toward aimAngle at turnRate:
       - If angleDiff < turnRate → snap to exact angle
       - Else → step turnRate toward target
       - Set MODELCONDITION_TURRET_ROTATE while turning
    5. If allowsPitch:
       - Compute pitch angle to target position (ASin(dz/distance))
       - Apply groundUnitPitch adjustment for nearby targets
       - Clamp to minPitch
       - Turn toward desiredPitch at pitchRate
    6. If angle and pitch aligned AND target in weapon range:
       → STATE_SUCCESS (transition to FIRE)
```

### 7.5 Idle Scanning

```
IdleScanState: turret occasionally rotates to random angle within
[minIdleScanAngle, maxIdleScanAngle] of natural position.
Makes units appear alive/alert when idle.
```

### 7.6 Recentering

```
After losing target (TURRETAI_HOLD timeout → TURRETAI_RECENTER):
Turret rotates back to naturalTurretAngle at half turnRate.
```

---

## 8. Docking & Deployment System

### 8.1 Dock Protocol

Derived from `DockUpdate` in the Generals codebase. A simplified 4-phase protocol:

```
Phase 1: APPROACH
  ├── If approach slots exist: wait for available slot
  ├── Move to approach position
  └── Wait for clear-to-enter signal

Phase 2: ENTER
  ├── Move to enterPosition (entrance waypoint)
  └── Trigger MODELCONDITION_DOCKING

Phase 3: DOCKED
  ├── Move to dockPosition (interior position)
  ├── Execute action():
  │     For SupplyWarehouse: give one box → docked unit gains supply
  │     For SupplyCenter:    take boxes → convert to money
  │     For Barracks:        heal infantry over time
  │     For RepairPad:       repair vehicle over time
  └── Wait for action completion

Phase 4: EXIT
  ├── Move to exitPosition (exit waypoint)
  ├── Clear MODELCONDITION_DOCKING
  └── Unit resumes normal AI
```

### 8.2 Dock Types

| Dock Type | Action | Provider |
|---|---|---|
| `SupplyWarehouseDock` | Transfers supply boxes to collector unit | Warehouse |
| `SupplyCenterDock` | Collects boxes, converts to player money | Refinery / Processing Plant |
| `HealDock` | Heals infantry over time | Barracks / Medical Tent |
| `RepairDock` | Repairs vehicles over time | Repair Pad / Service Depot |

### 8.3 Deploy/Undeploy (Buildings)

Structures follow a deploy lifecycle:

```
1. Player places building template on valid terrain
2. Builder unit (worker/dozer) moves to build site
3. Builder plays build animation while structure health increases
4. Structure at full health → MODELCONDITION transitions from BUILDING to normal
5. If damaged during build → construction pauses, builder repairs
6. If destroyed during build → rubble remains
```

For deployable units (e.g., siege units that unpack):

```
Deploy:
  ├── Unit must be still (speed = 0)
  ├── Play deploy animation (MODELCONDITION_DEPLOYED set)
  ├── During deploy: cannot move, weapons may be temporarily disabled
  └── Deploy complete: weapons enabled, unit immobilized

Undeploy:
  ├── Reverse deploy animation
  ├── Clear MODELCONDITION_DEPLOYED
  └── Unit can move again
```

---

## 9. Resource Gathering System

### 9.1 Simplified Supply Cycle

Based on the Generals `SupplyWarehouse → SupplyTruck → SupplyCenter → Money` cycle:

```
┌───────────────────┐
│  Spice Field /    │  (static resource on map)
│  Supply Depot     │  Has N boxes of resource
│  (Warehouse)      │
└─────────┬─────────┘
          │ Collector docks: gainOneBox()
          ▼
┌───────────────────┐
│  Harvester /      │
│  Carryall         │  Carries 0..MaxBoxes
│  (Supply Unit)    │
└─────────┬─────────┘
          │ Docks at refinery: loseOneBox()
          ▼
┌───────────────────┐
│  Refinery /       │
│  Processing Plant │  Converts boxes → credits
│  (Supply Center)  │
└─────────┬─────────┘
          │ Money.deposit(value)
          ▼
┌───────────────────┐
│  Player Credits   │  m_money += boxValue
└───────────────────┘
```

### 9.2 Gather Unit State Machine

```
IDLE
  └── findBestWarehouse() ──▶ MOVING_TO_SOURCE
                                  │ arrive at source
                                  ▼
                              DOCKING_AT_SOURCE
                                  │ docked: gainBoxes up to maxBoxes
                                  ▼
                              MOVING_TO_REFINERY
                                  │ arrive at refinery
                                  ▼
                              DOCKING_AT_REFINERY
                                  │ docked: loseBoxes → money to player
                                  ▼
                              (if auto-repeat: loop to MOVING_TO_SOURCE)
                              (else: IDLE)
```

### 9.3 Key Parameters

| Parameter | Description |
|---|---|
| `maxBoxes` | Maximum resource units carried at once |
| `warehouseScanDistance` | Radius to search for nearest source |
| `dockDelay` | Frames to spend docked (animation) |
| `boxValue` | Credits per resource box |

---

## 10. Camera System

### 10.1 Standard RTS Camera

A tactical RTS camera with pan, zoom, and rotate:

```
Camera properties:
  ├── Position:      (x, y) pivot on terrain + heightAboveGround (z)
  ├── Pitch:         Angle from horizontal (0..~80 degrees)
  ├── Yaw/Rotation:  Compass angle (0 = North)
  ├── Zoom:          Distance/vheight equivalent
  └── Field of View: Fixed perspective FOV
```

### 10.2 Camera Controls

| Input | Action |
|---|---|
| Arrow keys / WASD | Pan camera |
| Mouse at screen edge | Auto-scroll |
| Right mouse button drag | Drag-scroll (reverse direction) |
| Middle mouse button drag | Rotate camera yaw |
| Mouse scroll wheel | Zoom in/out |
| Ctrl+F1..F4 | Save camera bookmark |
| F1..F4 | Restore camera bookmark |

### 10.3 Camera Parameters

| Parameter | Default | Description |
|---|---|---|
| `minHeight` | 20 | Lowest camera altitude |
| `maxHeight` | 500 | Highest camera altitude (max zoom out) |
| `defaultPitch` | ~55° (0.96 rad) | Default pitch angle |
| `defaultAngle` | 0° (facing North-East) | Default rotation |
| `horizontalScrollSpeed` | ~2000 px/s | Pan speed X |
| `verticalScrollSpeed` | ~1800 px/s | Pan speed Y |
| `zoomSpeed` | varies | Zoom rate per scroll wheel tick |
| `scrollEdgeSize` | 10 px | Screen edge zone for auto-scroll |

### 10.4 Coordinate Mapping

```
World → Screen:
    worldToScreen(worldX, worldY, worldZ) → (screenX, screenY)

Screen → Terrain:
    screenToTerrain(screenX, screenY) → (worldX, worldY, terrainZ)

Object Picking:
    pickObject(screenX, screenY) → ObjectID or null
```

### 10.5 Camera Shake

```
shake(epicenterPos, intensity):
    Apply random offset to camera pivot each frame
    Range scales by distance from epicenter
    Decays over time to zero offset
```

---

## 11. Selection System

### 11.1 Selection Rectangle

```
Mouse Button Down:
    ├── Start tracking drag rectangle
    └── Deselect all if not holding Shift

Mouse Move (while held):
    └── Draw selection rectangle on screen

Mouse Button Up:
    ├── If rectangle small (single click): pickObject(screenX, screenY)
    ├── Else: iterate objects in screen-space rectangle
    │     ├── Filter: CanBeSelected == true
    │     ├── Filter: owned by current player (or allies if spectator)
    │     └── Add to selection set
    └── Update UI (command card, health bars)
```

### 11.2 Selection Rules

| Rule | Description |
|---|---|
| Single type | If selection contains only combat units → show combat commands |
| Mixed type | If selection mixed → show common commands (move, stop) |
| Double-click | Select all visible units of same type on screen |
| Shift+click | Toggle individual unit selection |
| Shift+drag | Add to selection (union) |
| Ctrl+1..9 | Assign selection to control group |
| 1..9 | Select control group |

---

## 12. Particle System (Simplified)

### 12.1 Particle Emitter Data

| Parameter | Type | Description |
|---|---|---|
| `maxParticles` | int | Maximum live particles |
| `emitRate` | float | Particles per second |
| `burstSize` | int | Particles per emission burst |
| `lifetime` | float | Particle lifespan (seconds) |
| `startSize` | float | Initial particle size (world units) |
| `endSize` | float | Final particle size (world units) |
| `startColor` | (r,g,b,a) | Initial color/opacity |
| `endColor` | (r,g,b,a) | Final color/opacity |
| `velocity` | (vx,vy,vz) | Base emission velocity |
| `velocityRandom` | (vx,vy,vz) | Random velocity offset range |
| `acceleration` | (ax,ay,az) | Constant acceleration (gravity) |
| `texture` | string | Sprite texture file path |
| `blendMode` | enum | ADDITIVE, ALPHA, MULTIPLY |
| `groundAlign` | bool | Align particles to terrain surface |
| `windAffects` | bool | Apply global wind vector |
| `rotationSpeed` | float | Particle spin rate |

### 12.2 Per-Particle State

```
struct Particle:
    position:    (x, y, z)
    velocity:    (vx, vy, vz)
    age:         seconds since creation
    lifeFraction: age / maxLifetime   (0 → 1)
    color:       lerp(startColor, endColor, lifeFraction)
    size:        lerp(startSize, endSize, lifeFraction)
    angle:       accumulated rotation over lifetime
```

### 12.3 Particle Emission Cycle

```
1. Emit(frame):
    elapsed = timeSinceLastEmit
    if elapsed > (1.0 / emitRate):
        count = floor(elapsed * emitRate)
        for i in 0..count:
            spawn particle:
                pos = emitter.position + randomPositionOffset
                vel = emitter.velocity + randomVelocityOffset
                age = 0

2. Update(frame):
    for each particle:
        age += deltaTime
        if age > lifetime: remove particle
        vel += acceleration * deltaTime
        pos += vel * deltaTime
        lifeFraction = age / lifetime
        color = lerp(startColor, endColor, lifeFraction)
        size = lerp(startSize, endSize, lifeFraction)
        angle += rotationSpeed * deltaTime

3. Render(frame):
    for each visible particle:
        batch as billboarded sprite at pos, with color, size, angle
        blend via blendMode
```

### 12.4 Particle Effect Types (Key Only)

| Effect            | Trigger                     | Typical Parameters                                                    |
| ----------------- | --------------------------- | --------------------------------------------------------------------- |
| **Gun Flash**     | Weapon fires                | burstSize=1, lifetime=0.1s, additive blend, bright yellow→transparent |
| **Muzzle Smoke**  | Weapon fires                | emitRate=20/s, lifetime=0.5s, alpha blend, dark grey→transparent      |
| **Explosion**     | Projectile detonates        | burstSize=30, lifetime=0.3-1.0s, additive, orange→black, expanding    |
| **Building Dust** | Structure damaged           | emitRate=10/s, lifetime=2s, alpha, brown→transparent, wind-affected   |
| **Death Smoke**   | Unit dies                   | emitRate=15/s, lifetime=3s, alpha, black→transparent, rising          |
| **Trail/Exhaust** | Projectile in flight        | continuous, lifetime=0.3s, additive, colored→transparent              |
| **Impact Spark**  | Bullet hits surface         | burstSize=3, lifetime=0.2s, additive, bright→transparent              |
| **Fire**          | Flamethrower/burning        | emitRate=30/s, lifetime=0.8s, additive, orange→red→black              |
| **Engine smoke**  | Unit moves, Factory working | lifetime 1s, scale grows, transparency grows                          |

### 12.5 Particle System Mappings to Game Events

```
Weapon Fires:
  → Gun Flash particle at muzzle bone
  → Muzzle Smoke at muzzle bone
  → Sound: fireSound

Projectile Impacts:
  → Explosion/Impact Spark at contact point
  → View shake (camera.shake(epicenter, intensity))
  → Sound: detonationSound

Unit Dies:
  → Death Smoke at unit position
  → If EXPLODED deathType: Explosion particles
  → Sound: deathSound
```

---

## 13. Data Configuration Format

### 13.1 Unit Definition (TOML equivalent of INI)

```toml
# tank_config.toml - Full Tiberian Sun inspired configuration

[identity]
name = "HeavyTank"
display_name = "Heavy Tank"          # Name shown in UI
category = "AFV"                       # Soldier, Civilian, VIP, Ship, Recon, AFV, IFV, LRFS, Support, Transport, AirPower, AirLift
owner = "Republic"                     # For now only Republic or Civilian, later more could be added
nominal = false                        # Always use given name rather than "enemy object"
insignificant = false                  # Don't announce when destroyed
legal_target = true                    # Allowed to be combat target
selectable = true                      # Can be selected by player
radar_visible = true                   # Visible on radar even under shroud
radar_invisible = false                # Invisible on radar maps
cloakable = false                      # Equipped with cloaking device
cloak_stop = false                     # Cloak when stopped moving

[stats]
strength = 400                         # Hit points
armor = "heavy"                        # none, wood, light, heavy, concrete
sight = 6                              # Sight range in cells
guard_range = 8                        # Distance to scan for enemies (default = weapon range)
speed = 5.0                            # Movement speed
rot = 5                                # Rate of turn (degrees per second)
cost = 1000                            # Cost in credits
points = 25                            # Scoring value
tech_level = 7                         # Tech level required (-1 = can't build). Just a stub for now, later could be rethought to use tech tree instead
build_limit = -1                       # Maximum allowed to build per house (-1 = unlimited)
build_time = 12.0                      # Seconds to build

[prerequisites]
prerequisite = ["barracks", "weapons_factory"]  # Buildings needed before production, in future could require more complex conditions

# In future this could be discarded or re-thought.
# A basic idea behind this is maing unit more powerful for destoying enemy units
[veterancy]
trainable = true                       # Can become veteran by experience
veteran_abilities = ["FASTER", "STRONGER", "FIREPOWER", "SIGHT"]
elite_abilities = ["ROF", "SELF_HEAL", "SCATTER"]

[combat]
primary_weapon = "125mm_cannon"
secondary_weapon = "coaxial_mg"
elite_primary = "125mm_cannon_elite"   # Weapon when elite
elite_secondary = "coaxial_mg_elite"
ammo = -1                              # Number of rounds (-1 = unlimited)
reload_time = 0.0                      # Time between reloads
manual_reload = false                  # Must reload by coordinating with reloader building
fire_angle = 64                        # Pitch of projectile launch (64 = horizontal, 0 = vertical)
target_laser = false                   # Has targeting laser
deploy_to_fire = false                 # Must deploy before firing
no_moving_fire = false                 # Must stop before firing

[abilities]
deploy = {
    enabled = false,
    deploy_time = 0.5,                 # Time in minutes to deploy/undeploy
    on_deploy = [
        { type = "enable_weapon", weapon = "primary" },
        { type = "modify_stat", stat = "speed", value = 0 }
    ]
}

docking = {
    enabled = false,
    dock_type = "refinery",            # Preferred docking building
    unload_time = 1.0,
    resource_capacity = 1000
}

[passengers]
passengers = 0                         # Number of passengers it may carry, 0 by default

[audio]
voice_select = ["tank_select_1", "tank_select_2"]
voice_move = ["tank_move_1", "tank_move_2"]
voice_attack = ["tank_attack_1", "tank_attack_2"]
voice_die = ["tank_die_1"]
voice_feedback = ["tank_hit_1"]
voice_comment = ["tank_idle_1", "tank_idle_2"]  # Idle voices
crush_sound = "crush_tank"             # Sound when crushed

[movement]
locomotor = "tracks"                   # tracks, wheels, walker...
crushable = false                      # Can be crushed by heavy tracked vehicles
crusher = true                         # Can crush infantry
carries_crate = false                  # Drops crate when destroyed

[turrets]
turret = true                          # Equipped with turret, false by default, but will be set to true if imported model has 'turret' node
turret_spins = false                   # Turret spins idly
turret_rotation_yaw = [-180, 180]
turret_rotation_speed = 5.0            # ROT for turret (same as body if not specified)
barrel_elevation_pitch = [-5, 45]
barrel_speed = 4.0

[building_interactions]  # For harvester/refinery relationships
dock = "refinery"                      # Preferred docking building
free_unit = "harvester"                # Free unit given when building built (for refinery)
unit_reload = false                    # Building reloads units on dock
unit_repair = false                    # Building repairs units on dock
dock_unload = false                    # Should unload when docking

[special_flags]
harvester = false                      # Special resource harvesting rules


```

### 13.2 Weapon Definition

```toml
Weapon:
# weapons/artillery_cannon.toml
# Can be defined separately to be reusable or
# in unit definition if used only by given unit
[weapon]
name = "155mm_howitzer"
type = "projectile"
projectile_scene = "artillery_shell.glb"
damage = 150
damage_type = "explosive"
area_damage = true
explosion_radius = 3.0
range = 25.0
cooldown = 4.0
min_range = 5.0          # can't fire too close
ballistic_arc = 0.5      # 0 = flat, 1 = high arc
accuracy = 0.95          # chance to hit, rest is scatter
scatter_radius = 1.5
particle_speed = 12      # speed of shell moving through the arc. Should speed and arc be one parameter?
shooting_correction = true # if particle-based weapon with ballistic arc, like artillery, will try to calculate where target would be when shell will hit the ground
homing = false           # true for weapons like guided missles  
```

### 13.3 Particle System Definition

```toml
[ParticleSystem]
name: "MuzzleFlash_Rifle"
maxParticles: 20
emitRate: 0               # one-shot burst
burstSize: 5
lifetime: 0.1
startSize: 0.5
endSize: 0.1
startColor: [1.0, 0.8, 0.2, 1.0]    # yellow, opaque
endColor:   [1.0, 0.6, 0.0, 0.0]    # orange, transparent
velocity: [0, 1.0, 0]
velocityRandom: [0.2, 0.3, 0.2]
texture: "fx_flash.png"
blendMode: ADDITIVE
```

---


### 14. Blender asset pipeline

Aimed at simplifying unit creation as much as possible. Just Blender-exported glb file with simple hierarchy and marked up with empties

### 14.1 Naming Convention for Units (Vehicles)

**Required structure:**

```
text

UnitRoot (Empty plain axes)
├── Chassis (hull, body) (Mesh)
├── Turret (Mesh, optional)
├── TurretPivot (Empty plain axes, position determines rotation center)
├── Barrel (Mesh, optional for artillery)
├── BarrelPivot (Empty plain axes)
├── Muzzle (Empty plain axes)
├── Wheel_FL, Wheel_FR, Wheel_RL, Wheel_RR (Mesh, optional for wheeled vehicles)
├── DockingPoint (Empty plain axes, for harvesters)
├── Tracks (Road dust emission point (optional, can be automated in future))
└── Smoke (Engine smoke emission point, optional)
```

All names are checked ignore case
**Rules:**

- Chassis must have collision mesh calculated from geometry    
- TurretPivot Y-axis determines turret rotation plane    
- BarrelPivot X-axis determines elevation (for artillery)    
- Wheel meshes can be animated via script based on movement speed    

### 14.2 Naming Convention for Buildings

**Required structure:**

```
text

BuildingRoot (Empty plain axes)
├── Base (Mesh)
├── Turret (Mesh, optional for defense)
├── TurretPivot (Empty plain axes)
├── SpawnPoint (Empty plain axes - where new units appear)
├── ExitPoint (Empty plain axes - where units drive out)
├── DockPoint (Empty plain axes - for harvesters/aircraft)
├── Smoke (Factory smoke emission point, optional)
└── Ramp (Mesh, optional - for vehicle deployment)
```

### 14.3 Texture Atlas Convention

- Keep this simple! Modder should be able to start just dropping red or black 512x512 png on model he created
- Later - use single 2K PBR atlas 
- Only albedo strictly required, but metallic+roughness+normal could be supported in future 
	- That's natively supported by Godot, so having naming conventions for png will be enough - heavy_tank00.png for albedo, heavy_tank00_normal.png for normal map etc.
- Naming: `unitname_atlas.png` and `unitname_atlas.glossy.png`    
- UV mapping: 0-1 space, shell packing by mesh part
## 15. Terrain System

### 15.1 Core Principle: Height Field, Not Collision Mesh

Terrain is treated exclusively as a **height field**. The rendering mesh (triangles arranged as quads for GPU) and the collision/query interface are separate concepts:

```
Terrain Data (2D array of heights)
  ├── Renderer       → JME Mesh (deterministic mesh generation from height field)
  ├── Query API      → TerrainHeightProvider.getHeight(x, z) → y
  └── Placement API  → isBuildable(x, z, footprint), isWalkable(x, z)
```

All game systems (pathfinding, movement, camera, object placement) query height through `TerrainHeightProvider.getHeight(x, z)` only. No system ever touches individual triangles for collision. This keeps the simplified radial/AABB collision model entirely independent of terrain complexity.

### 15.2 Heightmap Source

Terrain is generated from a **16-bit grayscale PNG heightmap**:

```
Format:           16-bit grayscale PNG (single-channel)
Size:             Power-of-two (e.g. 257×257, 513×513) for LOD compatibility
Convention:       Black (0x0000) = lowest height (lake bottom)
                  White (0xFFFF) = highest peak
World scale:      1 height unit per pixel?  Configurable: heightScale parameter
Map dimensions:   heightmapWidth × cellSize  by  heightmapHeight × cellSize
                  default cellSize = 1 world unit
```

Loading flow:
```
1. Read PNG → int[][] heights (0..65535)
2. Normalize to world heights: worldHeight = (pixelValue / 65535.0) * heightRange
3. Build height field array: float[width][height] 
4. Optionally: apply smoothing filter (3×3 box blur) to reduce stepping
5. Generate render mesh and feed TerrainHeightProvider
```

### 15.3 Elevation Levels (Tiberian Sun 2D Concept in 3D)

Terrain uses **3–4 discrete elevation levels** with smooth interpolation for visual transitions, matching Tiberian Sun's 2D approach:

| Level | Height (world units) | Visual | Game Effect |
|---|---|---|---|
| 0 — Water/Lake | 0.0 | Flat blue water surface | BLOCKED for non-hover units, hover units can pass |
| 1 — Low ground | 1.0 | Sand/grass floor | CLEAR, base walking surface |
| 2 — Mid ground | 2.5 | Slightly elevated, rock outcrops | CLEAR, small height difference |
| 3 — High ground | 5.0 | Mesa tops, ridges | CLEAR, height advantage (future: LOS bonus) |

Between levels, terrain **slopes smoothly** over ~2–3 world units (bilinear interpolation from the heightmap). Slopes never exceed ~30°, so units can traverse them without special climbing logic.

**Design constraint**: The heightmap author designs for these ~4 levels. The `heightScale` parameter maps pixel values to these bands. Sharp cliffs > 45° are **not supported** — the heightmap is implicitly soft-clamped to max slope during import.

### 15.4 Mesh Generation

Terrain is rendered as a regular grid of **quads**, each split into two triangles:

```
w+1 vertices wide, h+1 vertices deep
(w × h quads) × 2 triangles per quad = w × h × 2 triangles

Vertex position: (x * cellSize, height[x][z], z * cellSize)
Normal:          computed from adjacent vertex heights (central differences)
UV:              (x / w, z / h) for texture tiling
```

LOD (Level of Detail) is not required initially. A single mesh of ~256×256 quads (131K triangles) is well within JME's budget for an RTS viewport. Future LOD can use JME's `LodControl` or a simple distance-based mesh swap.

### 15.5 Water Surfaces

Lakes are defined as **flat regions** in the heightmap at or below level 0:

```
Detection:  For each cell, if height(x,z) ≤ WATER_HEIGHT → mark as water
Rendering:  Semi-transparent blue plane at WATER_HEIGHT
Collision:  Non-hover units: CELL_BLOCKED. Hover units: CELL_CLEAR.
Pathfinding:Water cells are walkable only for LOCO_HOVER locomotor type.
```

Water surface is a separate flat mesh (single quad or grid) rendered at the water height with a transparent blue material + animated UV offset for wave effect.

### 15.6 Ground Clamping for Objects

Every game object (unit, building, tree, decoration) queries terrain height for Y-position:

```java
// Pseudo-code — exact Y depends on unit type
float groundY = terrainProvider.getHeight(worldX, worldZ);
if (unit.isStructure()) {
    // Building: lowest corner footprint determines Y
    groundY = terrainProvider.getHeight(footprintCornerMinXZ);
}
object.setY(groundY + unit.getPreferredHeight());
```

No physics-driven ground snapping. No raycast-per-vertex. Just one float lookup per object per frame (at most).

### 15.7 Object Placement Rules

Objects are placed on terrain using the collision model from Section 5 (radial/AABB only).

#### 15.7.1 Building Placement

```
fn canPlaceBuilding(buildingTemplate, worldX, worldZ, rotation):
    1. Compute world-space AABB from template's collision AABB + position + rotation
    2. For each cell overlapping the AABB footprint (on pathfinding grid):
        a. If cell is CELL_BLOCKED → fail (overlapping existing structure)
        b. If cell is water (height ≤ WATER_HEIGHT for non-hover) → fail
        c. Compute terrain height at building's 4 corners:
           h1 = terrain.getHeight(nearX, nearZ)
           h2 = terrain.getHeight(nearX, farZ)
           h3 = terrain.getHeight(farX, nearZ)
           h4 = terrain.getHeight(farX, farZ)
        d. If |any h - any other h| > MAX_SLOPE → fail (terrain too uneven)
    3. Mark all footprint cells as CELL_BLOCKED
    4. Snap building Y to average of 4 corner heights
    5. Return success
```

**MaxSlope** parameter (default: 1.0 world unit height difference across the building's footprint). This ensures buildings sit flat even on slightly sloped terrain.

#### 15.7.2 Tree / Decoration Placement

Trees and neutral objects are **static environment** (no collision, no pathfinding interaction except visual):

```
fn scatterVegetation(terrainProvider, density):
    1. For each cell on the map at interval (density):
        a. Get cell center (wx, wz)
        b. height = terrain.getHeight(wx, wz)
        c. If height is water level → skip
        d. If height is too steep (gradient > threshold) → skip
        e. Pick random tree variant from palette
        f. Place tree mesh at (wx, height, wz) with random Y-rotation
        g. tree.mesh is a simple cross-quad or low-poly fbx/glb
    2. Hand-placed "hero trees" and rock formations are placed via map editor
```

For pathfinding, trees are either:
- **Non-blocking** (visual only, units pass through them — simpler, recommended initially)
- **CELL_BLOCKED** (if tree collision is needed later — mark 1 cell per tree)

#### 15.7.3 Civilian / Neutral Buildings

Same placement logic as player buildings. They are loaded from the map definition (not built at runtime):

```
fn placeNeutralBuildings(mapData):
    for each (buildingType, x, z, rotation) in mapData.buildings:
        if canPlaceBuilding(...):
            spawn building unit (neutral owner)
            mark grid cells as CELL_BLOCKED
```

### 15.8 TerrainHeightProvider Interface

```java
/**
 * Single interface for all terrain height queries.
 * All game systems depend on this interface, never on concrete terrain classes.
 */
public interface TerrainHeightProvider {
    /** @return terrain height at (worldX, worldZ). Thread-safe. */
    float getHeight(float worldX, float worldZ);

    /** @return true if terrain at this point is water (height ≤ waterLevel) */
    boolean isWater(float worldX, float worldZ);

    /** @return gradient magnitude at (worldX, worldZ) (0 = flat, higher = steeper) */
    float getGradient(float worldX, float worldZ);

    /** @return map bounds */
    float getMapMinX();
    float getMapMaxX();
    float getMapMinZ();
    float getMapMaxZ();
}
```

### 15.9 Two Implementations

| Implementation | Use Case | Behavior |
|---|---|---|
| `FlatTerrainProvider` | Stage 1, debug, quick-test maps | Returns constant 0 (or configured flat height) for all getHeight() calls |
| `HeightmapTerrainProvider` | Production game | Bilinear interpolation from 16-bit PNG heightmap array |

Both implement the same `TerrainHeightProvider` interface. Systems switch between them at startup based on map config.

---

## 16. UI Specification
Product should have several entry points, besides launching through main screen user should be able to start a map with plain polygon 1x1 km with one click and spawn units/buildings on it for quick testing

### 15.1 Main Menu Screen


```
+-------------------------------------------------+
|                    RTS GAME                      |
|                                                   |
|           [CAMPAIGN] [SKIRMISH] [SETTINGS]       |
|                                                   |
|    +---------------------------------------+     |
|    |  Available Maps:                      |     |
|    |  > Desert Hills (2-4 players)        |     |
|    |    Twin Rivers (2 players)           |     |
|    |    Canyon Assault (4 players)         |     |
|    +---------------------------------------+     |
|                                                   |
|  Player: Human    Faction: Republic    Color: Blue   |
|  Opponent: AI (Easy)                             |
|                                                   |
|                [START GAME]                       |
+-------------------------------------------------+
```

### 15.2 In-Game HUD Layout (Generals/Tiberian Sun style)

text

```
+-----------------------------------------------------------------------+
| [⚡ 1500] [🪨 800]                                      [MINIMAP]      |
|                                                          +-------+      |
|                                                          |       |      |
|                                          [Menu] [TacMap] |  MAP  |      |
|                                                          |       |      |
|                                                          +-------+      |
|                                                  [◂] [▸] [+/-]         |
|                                                                         |
|                        [3D GAME VIEW]                                  |
|                                                                         |
|                                                                         |
|                                                                         |
|                                                                         |
+---------------------------------------+--------------------------------+
| [Power Bar] [Health]                  |  BUILDING MENU                  |
|                                        | +----------------------------+ |
| [Selected Unit Info]                   | | War Factory (Ready)        | |
|                                        | | [L] [M] [H] [A]            | |
|                                        | +----------------------------+ |
|                                        |                                |
|                                        | | Barracks                   | |
|                                        | | [I] [J] [R]                | |
|                                        | +----------------------------+ |
+----------------------------------------+--------------------------------+
|                    [MINIMAP] (alternate position if bottom)             |
+-------------------------------------------------------------------------+
```

### 15.3 GUI Component Specifications

|Component|Position|Features|
|---|---|---|
|**Resource Bar**|Top-right|Animated counters for credits, ore, power|
|**Minimap**|Bottom-right|Clickable, fog-of-war overlay, pings|
|**Production Palette**|Right side|Scrollable grid, cooldown overlays, hotkeys 1-9|
|**Command Bar**|Bottom-center|Move/Attack/Stop/Deploy/Guard buttons|
|**Unit Info**|Bottom-left|Health bar, rank, current order, custom stats|
|**Power Indicator**|Top-left|Green/yellow/red bar with percentage|

### 15.4 Minimap Features
 
- **Structure icons**: Scaled squares (2x2 cells visible)    
- **Pings**: Animated rings on alert (enemy sighted)    
- **Tactical overlay**: Construction progress, radar sweep effect