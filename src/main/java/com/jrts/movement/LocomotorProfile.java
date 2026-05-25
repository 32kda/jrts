package com.jrts.movement;

import com.jrts.scene.TerrainHeightProvider;
import com.jrts.unit.Unit;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Runtime values derived from LocomotorConfig for a specific unit.
 */
public class LocomotorProfile {

    private final String type;
    private final float maxSpeed;
    private final float maxTurnRate;
    private final float closeEnoughDist;

    public LocomotorProfile(String type, float maxSpeed, float maxTurnRate, float closeEnoughDist) {
        this.type = type;
        this.maxSpeed = maxSpeed;
        this.maxTurnRate = maxTurnRate;
        this.closeEnoughDist = closeEnoughDist;
    }

    public String getType() {
        return type;
    }

    public float getMaxSpeed() {
        return maxSpeed;
    }

    public float getMaxTurnRate() {
        return maxTurnRate;
    }

    public float getCloseEnoughDist() {
        return closeEnoughDist;
    }
}
