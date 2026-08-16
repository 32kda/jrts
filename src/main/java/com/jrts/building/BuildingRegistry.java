package com.jrts.building;

import com.jme3.scene.Spatial;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Central registry of all live buildings, mirroring {@link com.jrts.unit.UnitRegistry}.
 */
public class BuildingRegistry {

    private static final Logger log = LoggerFactory.getLogger(BuildingRegistry.class);

    private static final String ENTITY_ID_KEY = "buildingId";

    private final Map<Integer, Building> byId = new HashMap<>();
    private final Map<Spatial, Building> bySpatial = new IdentityHashMap<>();

    public void register(Building building) {
        byId.put(building.getId(), building);
        bySpatial.put(building.getSpatial(), building);
        building.getSpatial().setUserData(ENTITY_ID_KEY, building.getId());
        log.debug("Registered building: id={}, name={}", building.getId(),
                building.getConfig().identity().name());
    }

    public void unregister(Building building) {
        byId.remove(building.getId());
        bySpatial.remove(building.getSpatial());
        log.debug("Unregistered building: id={}", building.getId());
    }

    public Optional<Building> findById(int id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Optional<Building> findBySpatial(Spatial spatial) {
        return Optional.ofNullable(bySpatial.get(spatial));
    }

    public List<Building> allBuildings() {
        return new ArrayList<>(byId.values());
    }

    public int count() {
        return byId.size();
    }
}
