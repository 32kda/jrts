# Ground Collision & Terrain Alignment

## 1. Height Query Pipeline

```
User code
  → TerrainLogic::getGroundHeight(x, y, &normal)
    → W3DTerrainLogic::getGroundHeight()
      → TheTerrainRenderObject->getHeightMapHeight(x, y, &normal)
        → BaseHeightMapRenderObjClass::getHeightMapHeight()
```

### Bilinear Height Interpolation

`BaseHeightMap.cpp:844` — converts world coords to grid indices, then bilinearly interpolates across 4 surrounding cells, selecting the correct triangle (upper/lower):

```cpp
float p0 = data[idx];
float p2 = data[idx + xExtent + 1];
if (fy > fx) // upper triangle
    height = (p3 + (1.0f-fy)*(p0-p3) + fx*(p2-p3)) * MAP_HEIGHT_SCALE;
else // lower triangle
    height = (p1 + fy*(p2-p1) + (1.0f-fx)*(p0-p1)) * MAP_HEIGHT_SCALE;
```

### Terrain Normal (12-point finite difference)

`BaseHeightMap.cpp:930` — samples 12 surrounding height values, computes smoothed slope on X and Y axes via cross product:

```cpp
Real deltaZ_X0 = d1-d11, deltaZ_X1 = d6-d0, deltaZ_X2 = d7-d3, deltaZ_X3 = d6-d0;
Real deltaZ_Y0 = d3-d4, deltaZ_Y1 = d2-d5, deltaZ_Y2 = d8-d1, deltaZ_Y3 = d9-d0;

// Bilinear interpolation of slope values
Real deltaZ_X = lerp(lerp(deltaZ_X0, deltaZ_X3, fx), lerp(deltaZ_X1, deltaZ_X2, fx), fy);
Real deltaZ_Y = lerp(lerp(deltaZ_Y0, deltaZ_Y3, fx), lerp(deltaZ_Y1, deltaZ_Y2, fx), fy);

// Cross product of slope vectors
l2r.Set(2*MAP_XY_FACTOR/MAP_HEIGHT_SCALE, 0, deltaZ_X);
n2f.Set(0, 2*MAP_XY_FACTOR/MAP_HEIGHT_SCALE, deltaZ_Y);
Vector3::Normalized_Cross_Product(l2r, n2f, &normalAtTexel);
```

---

## 2. Unit Z Clamping to Ground

`PhysicsUpdate.cpp:655-689` — each frame, ground height is queried at unit XY. If unit Z falls below, it snaps and velocity is zeroed:

```cpp
Real groundZ = TheTerrainLogic->getLayerHeight(mtx.Get_X_Translation(), mtx.Get_Y_Translation(), obj->getLayer());
if (mtx.Get_Z_Translation() <= groundZ) {
    Real dz = groundZ - mtx.Get_Z_Translation();
    m_vel.z += dz;
    if (m_vel.z > 0.0f) m_vel.z = 0.0f;
    mtx.Set_Z_Translation(groundZ);
    setFlag(ALLOW_TO_FALL, false);
} else if (mtx.Get_Z_Translation() > groundZ) {
    if (getFlag(STICK_TO_GROUND) && !getFlag(ALLOW_TO_FALL))
        mtx.Set_Z_Translation(groundZ);
}
```

### Locomotor Z Behaviors

`Locomotor.cpp:2180` — per `m_behaviorZ` setting:

| Type | Behavior |
|---|---|
| `Z_SMOOTH_RELATIVE_TO_HIGHEST_LAYER` | Applies lift force (spring) toward `surfaceHt + preferredHeight` |
| `Z_FIXED_SURFACE_RELATIVE_HEIGHT` | Directly sets `pos.z = surfaceHt + preferredHeight` |
| `Z_SEA_LEVEL` | Maintains fixed water-level Z |
| `Z_ABSOLUTE_HEIGHT` | Ignores terrain, fixed absolute Z |

### Death Ground Snap

`HeightDieUpdate.cpp:224`:
```cpp
if (modData->m_snapToGroundOnDeath || pos->z < terrainHeightAtPos)
    snap object Z to terrainHeightAtPos;
```

---

## 3. Unit Rotation / Terrain Slope Alignment

### Static Objects (buildings, immobile units)

`TerrainLogic.cpp:1507` — `alignOnTerrain()` builds an orthonormal matrix where Z follows terrain normal:

```cpp
void makeAlignToNormalMatrix(Real angle, const Coord3D& pos, const Coord3D& normal, Matrix3D& mtx) {
    Coord3D x, y, z;
    z = normal;
    x.x = Cos(angle); x.y = Sin(angle); x.z = 0;
    if (z.z != 0) {
        x.z = -(x.x*z.x + x.y*z.y) / z.z;  // orthogonalize
        x.normalize();
    }
    y.crossProduct(&z, &x, &y);
    y.normalize();
    mtx.Set(x.x, y.x, z.x, pos.x, x.y, y.y, z.y, pos.y, x.z, y.z, z.z, pos.z);
}
```

Applies to objects with `KINDOF_STICK_TO_TERRAIN_SLOPE` (immobile buildings).

### Vehicles (suspension model)

`Drawable.cpp:1775` — pitch and roll computed from terrain normal dot-products, then fed into a spring-damper system:

```cpp
// Compute ground pitch/roll from terrain normal
Coord3D normal;
TheTerrainLogic->getLayerHeight(pos->x, pos->y, obj->getLayer(), &normal);
Real groundPitch = (normal.x * dir->x + normal.y * dir->y) * (PI/2.0f);
Real groundRoll  = (normal.x * perp.x + normal.y * perp.y) * (PI/2.0f);

// Spring-damper model (Drawable.cpp:1848)
m_locoInfo->m_pitchRate += (-PITCH_STIFFNESS * (m_locoInfo->m_pitch - groundPitch))
                          - (PITCH_DAMPING * m_locoInfo->m_pitchRate);
m_locoInfo->m_rollRate += (-ROLL_STIFFNESS * (m_locoInfo->m_roll - groundRoll))
                         - (ROLL_DAMPING * m_locoInfo->m_rollRate);
m_locoInfo->m_pitch += m_locoInfo->m_pitchRate * UNIFORM_AXIAL_DAMPING;
m_locoInfo->m_roll  += m_locoInfo->m_rollRate  * UNIFORM_AXIAL_DAMPING;
```

---

## 4. Ground Collision Detection

### Convention: `other == nullptr` = ground

Used across all `CollideModule` implementations (`CollideModule.h:36`):

```cpp
// Note in the 'collide' method that 'other' can be null, this indicates a
// collision with the ground
```

### Trigger Points

| Trigger | Location |
|---|---|
| Landing after airborne | `PhysicsUpdate.cpp:707` — checks `WAS_AIRBORNE_LAST_FRAME` |
| Direct ground intersection | `NeutronMissileUpdate.cpp:520` — `!isAboveTerrain()` |
| Partition cell overlap | `PartitionManager::addPossibleCollisions()` — adds ground as collision candidate |

### Dispatch

```cpp
// PhysicsUpdate.cpp:1048
if (other == nullptr) {
    if (objContainedBy)
        objContainedBy->onCollide(other, loc, normal);
    return;
}

Object::onCollide(other, loc, normal)
  → for each CollideModule on the object:
      → module->onCollide(other, loc, normal)
```

---

## 5. Key Parameters

| Parameter | Source | Purpose |
|---|---|---|
| `MAP_XY_FACTOR` | BaseHeightMap.cpp | World units per grid cell |
| `MAP_HEIGHT_SCALE` | BaseHeightMap.cpp | Heightmap value → world units |
| `STICK_TO_GROUND` | PhysicsFlagsType | Snap Z to ground each frame |
| `KINDOF_STICK_TO_TERRAIN_SLOPE` | KindOf.h | Full matrix alignment to terrain normal |
| `PITCH_STIFFNESS` / `PITCH_DAMPING` | Drawable.cpp | Suspension spring constant / damping |
| `ROLL_STIFFNESS` / `ROLL_DAMPING` | Drawable.cpp | Suspension spring constant / damping |
| `preferredHeight` | LocomotorTemplate | Desired altitude above terrain |
| `surfaceHt` | Locomotor::handleBehaviorZ | Ground height at current XY |

---

## 6. Summary Flow (Vehicle Moving Over Terrain)

```
1. Per-frame PhysicsUpdate::update()
   ├── Apply locomotor forces (accel, turn)
   ├── Integrate velocity to position
   ├── getLayerHeight(x, y, &normal) — terrain height + normal
   ├── Clamp Z to terrain height (STICK_TO_GROUND)
   └── If airborne → detect landing → onCollide(nullptr, ...)

2. Per-frame Drawable::updateLocoInfo()
   ├── getLayerHeight(x, y, &normal)
   ├── Compute groundPitch/groundRoll from normal·dir / normal·perp
   └── Spring-damper → smooth pitch/roll visual

3. Result: vehicle follows terrain contours,
   Z locked to surface, visual tilt matches slopes
```
