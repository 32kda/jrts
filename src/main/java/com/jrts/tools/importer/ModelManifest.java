package com.jrts.tools.importer;

import java.util.*;

/**
 * Structured metadata for an imported model.
 * Serialized into the .m3o file header as JSON.
 */
public class ModelManifest {

    private String modelName;
    private String category;

    private Map<NodeRole, String> roles = new EnumMap<>(NodeRole.class);

    private CollisionShapeData collision;

    private List<AnimationClipInfo> animations = new ArrayList<>();
    private List<BoneSlotInfo> boneSlots = new ArrayList<>();

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Map<NodeRole, String> getRoles() {
        return Collections.unmodifiableMap(roles);
    }

    public void registerRole(NodeRole role, String nodePath) {
        roles.put(role, nodePath);
    }

    public String getNodePath(NodeRole role) {
        return roles.get(role);
    }

    public boolean hasRole(NodeRole role) {
        return roles.containsKey(role);
    }

    public CollisionShapeData getCollision() {
        return collision;
    }

    public void setCollision(CollisionShapeData collision) {
        this.collision = collision;
    }

    public List<AnimationClipInfo> getAnimations() {
        return Collections.unmodifiableList(animations);
    }

    public void addAnimation(AnimationClipInfo clip) {
        animations.add(clip);
    }

    public List<BoneSlotInfo> getBoneSlots() {
        return Collections.unmodifiableList(boneSlots);
    }

    public void addBoneSlot(BoneSlotInfo slot) {
        boneSlots.add(slot);
    }

    public record CollisionShapeData(
            String type,
            float[] center,
            float[] halfExtents,
            float radius) {
    }

    public record AnimationClipInfo(
            String name,
            float duration,
            boolean looping) {
    }

    public record BoneSlotInfo(
            String boneName,
            String slotType) {
    }
}
