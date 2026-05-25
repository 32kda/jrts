package com.jrts.input;

import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Bridges JME RawInputListener events to higher-level named actions.
 * No game logic here — just maps raw input to action strings.
 */
public class ActionMapper implements RawInputListener {

    private static final Logger log = LoggerFactory.getLogger(ActionMapper.class);

    private final CopyOnWriteArrayList<InputActionHandler> handlers = new CopyOnWriteArrayList<>();

    private boolean leftDown;
    private boolean rightDown;
    private boolean shiftDown;
    private boolean dragging;
    private boolean dragThresholdPassed;
    private float dragStartX;
    private float dragStartY;
    private float currentMouseX;
    private float currentMouseY;

    private static final float DRAG_THRESHOLD = 5f;

    public interface InputActionHandler {
        void onAction(String action, float screenX, float screenY, Modifiers mods);
    }

    public record Modifiers(boolean shift, boolean ctrl, boolean alt) {
        public static final Modifiers NONE = new Modifiers(false, false, false);

        public Modifiers withShift(boolean shift) {
            return new Modifiers(shift, ctrl, alt);
        }
    }

    public void addHandler(InputActionHandler handler) {
        handlers.add(handler);
    }

    public void removeHandler(InputActionHandler handler) {
        handlers.remove(handler);
    }

    private void dispatch(String action, float screenX, float screenY) {
        Modifiers mods = new Modifiers(shiftDown, false, false);
        for (InputActionHandler handler : handlers) {
            handler.onAction(action, screenX, screenY, mods);
        }
    }

    @Override
    public void onMouseButtonEvent(MouseButtonEvent evt) {
        currentMouseX = evt.getX();
        currentMouseY = evt.getY();

        if (evt.getButtonIndex() == MouseInput.BUTTON_LEFT) {
            leftDown = evt.isPressed();
            if (evt.isPressed()) {
                dragging = true;
                dragThresholdPassed = false;
                dragStartX = evt.getX();
                dragStartY = evt.getY();
                dispatch("SELECT_START", evt.getX(), evt.getY());
            } else {
                if (!dragThresholdPassed) {
                    dispatch("SELECT_CLICK", evt.getX(), evt.getY());
                }
                dispatch("SELECT_END", evt.getX(), evt.getY());
                dragging = false;
            }
        }

        if (evt.getButtonIndex() == MouseInput.BUTTON_RIGHT) {
            rightDown = evt.isPressed();
            if (!evt.isPressed()) {
                dispatch("ORDER_MOVE", evt.getX(), evt.getY());
            }
        }
    }

    @Override
    public void onMouseMotionEvent(MouseMotionEvent evt) {
        currentMouseX = evt.getX();
        currentMouseY = evt.getY();

        if (dragging && leftDown) {
            float dx = evt.getX() - dragStartX;
            float dy = evt.getY() - dragStartY;
            if (!dragThresholdPassed && Math.abs(dx) + Math.abs(dy) > DRAG_THRESHOLD) {
                dragThresholdPassed = true;
            }
            if (dragThresholdPassed) {
                dispatch("SELECT_MOVE", evt.getX(), evt.getY());
            }
        }
    }

    @Override
    public void onKeyEvent(KeyInputEvent evt) {
        if (evt.getKeyCode() == KeyInput.KEY_LSHIFT
                || evt.getKeyCode() == KeyInput.KEY_RSHIFT) {
            shiftDown = evt.isPressed();
        }
    }

    @Override
    public void onTouchEvent(TouchEvent evt) {
    }

    @Override
    public void onJoyButtonEvent(JoyButtonEvent evt) {
    }

    @Override
    public void onJoyAxisEvent(JoyAxisEvent evt) {
    }

    @Override
    public void beginInput() {
    }

    @Override
    public void endInput() {
    }

    public boolean isLeftDown() {
        return leftDown;
    }

    public boolean isRightDown() {
        return rightDown;
    }

    public boolean isShiftDown() {
        return shiftDown;
    }

    public boolean isDragging() {
        return dragging && dragThresholdPassed;
    }

    public float getDragStartX() {
        return dragStartX;
    }

    public float getDragStartY() {
        return dragStartY;
    }

    public float getCurrentMouseX() {
        return currentMouseX;
    }

    public float getCurrentMouseY() {
        return currentMouseY;
    }
}
