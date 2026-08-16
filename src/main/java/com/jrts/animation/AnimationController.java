package com.jrts.animation;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;

/**
 * Tracks which {@link ModelCondition}s are currently active and resolves them to a single
 * winning condition (highest priority) and its animation clip name.
 *
 * <p>Engine-independent: it only stores condition flags and a condition→clip-name mapping.
 * The JME layer ({@code BuildingAnimationControl}) reads {@link #currentClipName()} each
 * frame and drives the actual {@code AnimControl}. Clip names are injected, typically from a
 * {@code [animation]} config section; a condition without a clip simply resolves to empty.
 */
public class AnimationController {

    private final EnumSet<ModelCondition> active = EnumSet.noneOf(ModelCondition.class);
    private final Map<ModelCondition, String> clips = new EnumMap<>(ModelCondition.class);

    /**
     * Set or clear an active condition.
     */
    public void setActive(ModelCondition condition, boolean on) {
        if (on) {
            active.add(condition);
        } else {
            active.remove(condition);
        }
    }

    /**
     * Clear all active conditions.
     */
    public void clear() {
        active.clear();
    }

    public boolean isActive(ModelCondition condition) {
        return active.contains(condition);
    }

    /**
     * @return the highest-priority currently active condition ({@code IDLE} when none)
     */
    public ModelCondition currentCondition() {
        ModelCondition winner = ModelCondition.IDLE;
        for (ModelCondition condition : active) {
            if (condition.priority() > winner.priority()) {
                winner = condition;
            }
        }
        return winner;
    }

    /**
     * @return the clip name bound to the winning condition, if any
     */
    public Optional<String> currentClipName() {
        return clipNameFor(currentCondition());
    }

    /**
     * Bind a clip name to a condition (from config).
     */
    public void setClipName(ModelCondition condition, String clipName) {
        if (clipName == null || clipName.isBlank()) {
            clips.remove(condition);
        } else {
            clips.put(condition, clipName);
        }
    }

    public Optional<String> clipNameFor(ModelCondition condition) {
        return Optional.ofNullable(clips.get(condition));
    }
}
