package com.jrts.input;

/**
 * Tracks a screen-space drag rectangle for unit selection.
 * State machine: IDLE → DRAGGING → IDLE
 */
public class SelectionBox {

    public enum State {
        IDLE, DRAGGING
    }

    private State state = State.IDLE;
    private float startX;
    private float startY;
    private float currentX;
    private float currentY;

    public void start(float screenX, float screenY) {
        this.startX = screenX;
        this.startY = screenY;
        this.currentX = screenX;
        this.currentY = screenY;
        this.state = State.DRAGGING;
    }

    public void update(float screenX, float screenY) {
        if (state == State.DRAGGING) {
            this.currentX = screenX;
            this.currentY = screenY;
        }
    }

    public void end() {
        this.state = State.IDLE;
    }

    public boolean isActive() {
        return state == State.DRAGGING;
    }

    public boolean isClick() {
        float dx = currentX - startX;
        float dy = currentY - startY;
        return Math.abs(dx) + Math.abs(dy) < 5f;
    }

    public float getMinX() {
        return Math.min(startX, currentX);
    }

    public float getMaxX() {
        return Math.max(startX, currentX);
    }

    public float getMinY() {
        return Math.min(startY, currentY);
    }

    public float getMaxY() {
        return Math.max(startY, currentY);
    }

    public float getStartX() {
        return startX;
    }

    public float getStartY() {
        return startY;
    }

    public float getCurrentX() {
        return currentX;
    }

    public float getCurrentY() {
        return currentY;
    }

    public State getState() {
        return state;
    }
}
