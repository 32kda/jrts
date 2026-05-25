package com.jrts.selectable;

/**
 * Marker component for selectable entities.
 * Stage 1: just a marker; Stage 2+ ECS will use this as a component.
 */
public class SelectableComponent {

    private boolean selected;

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
