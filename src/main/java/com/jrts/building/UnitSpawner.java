package com.jrts.building;

import com.jrts.unit.Unit;
import com.jme3.math.Vector3f;

/**
 * Spawns a unit from its config type name at a world position. Implemented by the scene layer
 * (which resolves config + model); keeps {@link ProductionControl} free of config/model I/O.
 */
@FunctionalInterface
public interface UnitSpawner {

    /**
     * @param unitType config name (resolves to {@code assets/config/units/&lt;type&gt;.toml})
     * @param position spawn world position
     * @param yaw      initial facing, radians
     * @return the spawned unit, or {@code null} if it could not be spawned
     */
    Unit spawn(String unitType, Vector3f position, float yaw);
}
