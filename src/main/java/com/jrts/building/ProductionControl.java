package com.jrts.building;

import com.jrts.movement.NavigationService;
import com.jrts.unit.Unit;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Building production queue, attached to the building spatial.
 *
 * <p>Each frame it dequeues one unit type, spawns it at the building's {@code SpawnPoint},
 * and issues a move order to the {@code ExitPoint} (falling back to {@code Ramp}, then the
 * spawn point) so the new unit drives out of the building. Spawning is delegated to a
 * {@link UnitSpawner}; pathing to a {@link NavigationService}, keeping this control free of
 * config/model I/O and trivially testable.</p>
 */
public class ProductionControl extends AbstractControl {

    private static final Logger log = LoggerFactory.getLogger(ProductionControl.class);

    private final Building building;
    private final UnitSpawner spawner;
    private final NavigationService navigation;
    private final Deque<String> queue = new ArrayDeque<>();

    public ProductionControl(Building building, UnitSpawner spawner, NavigationService navigation) {
        this.building = building;
        this.spawner = spawner;
        this.navigation = navigation;
    }

    /**
     * Queue a unit type for production.
     */
    public void enqueue(String unitType) {
        queue.addLast(unitType);
    }

    /**
     * Queue several unit types (config's {@code production.produces} list).
     */
    public void enqueueAll(List<String> unitTypes) {
        queue.addAll(unitTypes);
    }

    public int queueSize() {
        return queue.size();
    }

    public boolean isProducing() {
        return !queue.isEmpty();
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (spatial != null) {
            processQueue();
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        // No custom rendering.
    }

    void processQueue() {
        if (queue.isEmpty()) {
            return;
        }
        Node spawnPoint = building.getSpawnPoint();
        if (spawnPoint == null) {
            log.warn("Building {} has no SpawnPoint; cannot produce units", building.getId());
            queue.clear();
            return;
        }

        String type = queue.removeFirst();
        Vector3f spawnPos = spawnPoint.getWorldTranslation();
        Unit unit = spawner.spawn(type, spawnPos, building.getBodyYaw());
        if (unit == null) {
            return;
        }
        unit.setWaypoints(navigation.computePath(spawnPos, resolveExit()));
    }

    private Vector3f resolveExit() {
        if (building.getExitPoint() != null) {
            return building.getExitPoint().getWorldTranslation();
        }
        if (building.getRampPoint() != null) {
            return building.getRampPoint().getWorldTranslation();
        }
        return building.getSpawnPoint().getWorldTranslation();
    }
}
