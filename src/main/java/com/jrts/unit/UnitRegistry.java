package com.jrts.unit;

import com.jme3.scene.Spatial;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Central registry of all live units. Provides:
 * - O(1) lookup by runtime ID
 * - O(1) lookup by JME Spatial (via UserData back-link)
 * - Iteration over all units
 * - Iteration filtered by flag mask
 */
public class UnitRegistry {

    private static final Logger log = LoggerFactory.getLogger(UnitRegistry.class);

    private static final String ENTITY_ID_KEY = "entityId";

    private final Map<Integer, Unit> byId = new HashMap<>();
    private final Map<Spatial, Unit> bySpatial = new IdentityHashMap<>();

    public void register(Unit unit) {
        byId.put(unit.getId(), unit);
        bySpatial.put(unit.getSpatial(), unit);
        unit.getSpatial().setUserData(ENTITY_ID_KEY, unit.getId());
        log.debug("Registered unit: id={}, name={}", unit.getId(), unit.getConfig().identity().name());
    }

    public void unregister(Unit unit) {
        byId.remove(unit.getId());
        bySpatial.remove(unit.getSpatial());
        log.debug("Unregistered unit: id={}", unit.getId());
    }

    public Optional<Unit> findById(int id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Optional<Unit> findBySpatial(Spatial spatial) {
        return Optional.ofNullable(bySpatial.get(spatial));
    }

    /**
     * Find a unit by looking up the entityId UserData on a spatial.
     */
    public Optional<Unit> findByEntityId(Spatial spatial) {
        Integer entityId = spatial.getUserData(ENTITY_ID_KEY);
        if (entityId != null) {
            return findById(entityId);
        }
        return Optional.empty();
    }

    public List<Unit> allUnits() {
        return new ArrayList<>(byId.values());
    }

    public List<Unit> unitsWithFlag(int flag) {
        List<Unit> result = new ArrayList<>();
        for (Unit unit : byId.values()) {
            if ((unit.getFlags() & flag) != 0) {
                result.add(unit);
            }
        }
        return result;
    }

    public int count() {
        return byId.size();
    }
}
