package com.jrts.input;

import com.jrts.movement.NavigationService;
import com.jrts.unit.Unit;
import com.jme3.math.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Translates right-click actions into orders for the currently selected units.
 *
 * Stage 1 orders:
 *   MOVE: right-click on terrain → set waypoint (straight line path)
 */
public class CommandDispatcher implements ActionMapper.InputActionHandler {

    private static final Logger log = LoggerFactory.getLogger(CommandDispatcher.class);

    private final SelectionSystem selectionSystem;
    private final MousePicker mousePicker;
    private final NavigationService navigationService;

    public CommandDispatcher(SelectionSystem selectionSystem, MousePicker mousePicker,
                             NavigationService navigationService) {
        this.selectionSystem = selectionSystem;
        this.mousePicker = mousePicker;
        this.navigationService = navigationService;
        log.info("CommandDispatcher initialized");
    }

    @Override
    public void onAction(String action, float screenX, float screenY,
                         ActionMapper.Modifiers mods) {
        if ("ORDER_MOVE".equals(action)) {
            Optional<Vector3f> terrain = mousePicker.pickTerrain(screenX, screenY);
            terrain.ifPresent(target -> {
                log.debug("Move order at ({:.1f},{:.1f},{:.1f})", target.x, target.y, target.z);
                for (Unit unit : selectionSystem.getSelected()) {
                    if (unit.canMove()) {
                        unit.setWaypoints(navigationService.computePath(
                                unit.getPosition(), target));
                    }
                }
            });
        }
    }
}
