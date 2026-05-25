package com.jrts.input;

import com.jrts.unit.Unit;
import com.jrts.unit.UnitRegistry;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages the current selection set.
 *
 * Single source of truth for "which units are selected".
 * Fires SelectionChangedEvent to observers (UI update, highlight toggle).
 */
public class SelectionSystem implements ActionMapper.InputActionHandler {

    private static final Logger log = LoggerFactory.getLogger(SelectionSystem.class);

    private final UnitRegistry registry;
    private final MousePicker mousePicker;
    private final SelectionBox selectionBox;
    private final Camera cam;
    private final int screenWidth;
    private final int screenHeight;

    private final Set<Unit> selectedUnits = new LinkedHashSet<>();
    private final List<SelectionObserver> observers = new CopyOnWriteArrayList<>();

    public interface SelectionObserver {
        void onSelectionChanged(Set<Unit> selected);
    }

    public SelectionSystem(UnitRegistry registry, MousePicker mousePicker,
                           SelectionBox selectionBox, Camera cam,
                           int screenWidth, int screenHeight) {
        this.registry = registry;
        this.mousePicker = mousePicker;
        this.selectionBox = selectionBox;
        this.cam = cam;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        log.info("SelectionSystem initialized");
    }

    @Override
    public void onAction(String action, float screenX, float screenY,
                         ActionMapper.Modifiers mods) {
        switch (action) {
            case "SELECT_START" -> {
                selectionBox.start(screenX, screenY);
                if (!mods.shift()) {
                    clearSelection();
                }
            }
            case "SELECT_MOVE" -> selectionBox.update(screenX, screenY);
            case "SELECT_CLICK" -> handleClickSelect(screenX, screenY, mods.shift());
            case "SELECT_END" -> {
                if (selectionBox.isActive() && !selectionBox.isClick()) {
                    handleRectangleSelect();
                }
                selectionBox.end();
            }
        }
    }

    private void handleClickSelect(float screenX, float screenY, boolean shift) {
        Optional<Unit> pickedUnit = mousePicker.pickUnit(screenX, screenY);

        if (pickedUnit.isPresent()) {
            Unit unit = pickedUnit.get();
            if (shift) {
                if (selectedUnits.contains(unit)) {
                    selectedUnits.remove(unit);
                } else {
                    selectedUnits.add(unit);
                }
            } else {
                selectedUnits.clear();
                selectedUnits.add(unit);
            }
        } else {
            if (!shift) {
                clearSelection();
            }
        }

        notifyObservers();
        log.debug("Selection: {} units selected", selectedUnits.size());
    }

    private void handleRectangleSelect() {
        float minX = selectionBox.getMinX();
        float maxX = selectionBox.getMaxX();
        float minY = selectionBox.getMinY();
        float maxY = selectionBox.getMaxY();

        for (Unit unit : registry.allUnits()) {
            if (!unit.isSelectable()) {
                continue;
            }

            Vector2f screenPos = com.jrts.camera.ScreenMap.worldToScreen(
                    unit.getPosition(), cam, screenWidth, screenHeight);

            if (screenPos.x >= minX && screenPos.x <= maxX
                    && screenPos.y >= minY && screenPos.y <= maxY) {
                selectedUnits.add(unit);
            }
        }

        notifyObservers();
        log.debug("Rectangle select: {} units in selection", selectedUnits.size());
    }

    public void clearSelection() {
        selectedUnits.clear();
        notifyObservers();
    }

    public Set<Unit> getSelected() {
        return Collections.unmodifiableSet(selectedUnits);
    }

    public boolean isSingleSelected() {
        return selectedUnits.size() == 1;
    }

    public Optional<Unit> getSingleSelected() {
        return selectedUnits.stream().findFirst();
    }

    public void addObserver(SelectionObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(SelectionObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers() {
        Set<Unit> snapshot = Collections.unmodifiableSet(new LinkedHashSet<>(selectedUnits));
        for (SelectionObserver observer : observers) {
            observer.onSelectionChanged(snapshot);
        }
    }
}
