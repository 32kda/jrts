package com.jrts.building;

import com.jrts.config.BuildingConfig;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

/**
 * Runtime building instance, mirroring {@link com.jrts.unit.Unit}: wraps a JME spatial and
 * holds game-level state (owner, health, facing). Buildings are static structures; they are
 * not selectable or movable in Stage 1.
 */
public class Building {

    private final int id;
    private final BuildingConfig config;
    private final Node spatial;

    private int owner;
    private int health;
    private float bodyYaw;

    private Node turretPivotNode;
    private Node barrelPivotNode;
    private Node muzzleNode;
    private Node spawnPointNode;
    private Node exitPointNode;
    private Node dockPointNode;
    private Node smokePointNode;
    private Node rampPointNode;

    public Building(int id, BuildingConfig config, Node spatial) {
        this.id = id;
        this.config = config;
        this.spatial = spatial;
        this.owner = 0;
        this.health = config.stats().health();
    }

    public int getId() {
        return id;
    }

    public BuildingConfig getConfig() {
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

    public int getOwner() {
        return owner;
    }

    public void setOwner(int owner) {
        this.owner = owner;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public float getBodyYaw() {
        return bodyYaw;
    }

    public void setBodyYaw(float yaw) {
        this.bodyYaw = yaw;
        spatial.setLocalRotation(new Quaternion().fromAngleAxis(yaw, Vector3f.UNIT_Y));
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

    public Node getSpawnPoint() {
        return spawnPointNode;
    }

    public void setSpawnPoint(Node node) {
        this.spawnPointNode = node;
    }

    public Node getExitPoint() {
        return exitPointNode;
    }

    public void setExitPoint(Node node) {
        this.exitPointNode = node;
    }

    public Node getDockPoint() {
        return dockPointNode;
    }

    public void setDockPoint(Node node) {
        this.dockPointNode = node;
    }

    public Node getSmokePoint() {
        return smokePointNode;
    }

    public void setSmokePoint(Node node) {
        this.smokePointNode = node;
    }

    public Node getRampPoint() {
        return rampPointNode;
    }

    public void setRampPoint(Node node) {
        this.rampPointNode = node;
    }

    @Override
    public String toString() {
        return "Building[id=" + id + ", name=" + config.identity().name()
                + ", pos=" + getPosition() + "]";
    }
}
