package com.jrts.docking;

/**
 * A dockable object, exposing the state a {@link DockAction} needs to read/write.
 *
 * <p>Implemented by units (and potentially buildings) so dock actions stay decoupled from
 * concrete entity classes.
 */
public interface DockTarget {

    int getHealth();

    void setHealth(int health);

    int getMaxHealth();
}
