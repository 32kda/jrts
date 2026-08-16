package com.jrts.unit;

import com.jrts.config.UnitConfig;
import com.jrts.docking.DockTarget;
import com.jrts.tools.importer.ModelManifest;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Runtime unit instance. Not an ECS entity yet — that comes in Stage 2.
 * For Stage 1, Unit is a plain object holding all state needed for
 * camera + selection + movement + turret.
 *
 * The JME Spatial is the visual representation. Unit wraps it
 * and provides game-level state (health, flags, selection status).
 */
public class Unit implements DockTarget {

    private final int id;
    private final UnitConfig config;
    private final Node spatial;
    private final int flags;

    private int owner;
    private float bodyYaw;
    private boolean selected;
    private int health;
    private List<Vector3f> waypoints;

    private Node turretPivotNode;
    private Node barrelPivotNode;
    private Node muzzleNode;

    private boolean wasAirborne;
    private boolean grounded = true;

    public Unit(int id, UnitConfig config, Node spatial, int flags) {
        this.id = id;
        this.config = config;
        this.spatial = spatial;
        this.flags = flags;
        this.owner = 0;
        this.health = config.stats().strength();
        this.waypoints = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public int getFlags() {
        return flags;
    }

    public int getOwner() {
        return owner;
    }

    public void setOwner(int owner) {
        this.owner = owner;
    }

    public UnitConfig getConfig() {
        return config;
    }

    public Node getSpatial() {
        return spatial;
    }

    public Vector3f getPosition() {
        return spatial.getWorldTranslation();
    }

    public void setPosition(Vector3f pos) {
        spatial.setLocalTranslation(pos);
    }

    public float getBodyYaw() {
        return bodyYaw;
    }

    public void setBodyYaw(float yaw) {
        this.bodyYaw = yaw;
        spatial.setLocalRotation(new Quaternion().fromAngleAxis(yaw, Vector3f.UNIT_Y));
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    @Override
    public int getMaxHealth() {
        return config.stats().strength();
    }

    public Node getTurretPivot() {
        return turretPivotNode;
    }

    public void setTurretPivot(Node node) {
        this.turretPivotNode = node;
    }

    public Node getBarrelPivot() {
        return barrelPivotNode;
    }

    public void setBarrelPivot(Node node) {
        this.barrelPivotNode = node;
    }

    public Node getMuzzle() {
        return muzzleNode;
    }

    public void setMuzzle(Node node) {
        this.muzzleNode = node;
    }

    public boolean canMove() {
        return (flags & UnitFlags.CAN_MOVE) != 0;
    }

    public boolean isSelectable() {
        return (flags & UnitFlags.SELECTABLE) != 0;
    }

    public boolean hasTurret() {
        return (flags & UnitFlags.HAS_TURRET) != 0;
    }

    public boolean isStickToGround() {
        return (flags & UnitFlags.STICK_TO_GROUND) != 0;
    }

    public boolean isAirborne() {
        return (flags & UnitFlags.AIRBORNE) != 0;
    }

    public boolean wasAirborne() {
        return wasAirborne;
    }

    public void setWasAirborne(boolean v) {
        this.wasAirborne = v;
    }

    public boolean isGrounded() {
        return grounded;
    }

    public void setGrounded(boolean v) {
        this.grounded = v;
    }

    public List<Vector3f> getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(List<Vector3f> waypoints) {
        this.waypoints = waypoints != null ? new ArrayList<>(waypoints) : new ArrayList<>();
    }

    /**
     * @return height above terrain (0 for ground units, >0 for hover/airborne)
     */
    public float getPreferredHeight() {
        return (flags & UnitFlags.AIRBORNE) != 0 ? 2.0f : 0.0f;
    }

    @Override
    public String toString() {
        return "Unit[id=" + id + ", name=" + config.identity().name()
                + ", pos=" + getPosition() + ", flags=0x"
                + Integer.toHexString(flags) + "]";
    }
}
