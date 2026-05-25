package com.jrts.movement;

import com.jrts.camera.ScreenMap;
import com.jrts.scene.TerrainHeightProvider;
import com.jme3.math.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Stage 1 mock implementation.
 * Returns a single waypoint: the destination itself.
 * No obstacle consideration.
 * Uses TerrainHeightProvider for map-bounds clamping.
 */
public class SimpleLineNavigation implements NavigationService {

    private static final Logger log = LoggerFactory.getLogger(SimpleLineNavigation.class);

    private final TerrainHeightProvider terrain;

    public SimpleLineNavigation(TerrainHeightProvider terrain) {
        this.terrain = terrain;
        log.info("SimpleLineNavigation initialized");
    }

    @Override
    public List<Vector3f> computePath(Vector3f start, Vector3f end) {
        Vector3f clamped = ScreenMap.clampToMap(end, terrain);
        log.debug("Path: ({:.1f},{:.1f},{:.1f}) → ({:.1f},{:.1f},{:.1f})",
                start.x, start.y, start.z, clamped.x, clamped.y, clamped.z);
        return Collections.singletonList(clamped);
    }
}
