package com.jrts.docking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DockStateMachineTest {

    private final DockStateMachine machine = new DockStateMachine(2f);

    @Test
    void startsIdle() {
        assertEquals(DockState.IDLE, machine.state());
        assertFalse(machine.isBusy());
    }

    @Test
    void fullCycleTransitionsInOrder() {
        machine.requestDock();
        assertEquals(DockState.APPROACH, machine.state());

        machine.onApproachReached();
        assertEquals(DockState.ENTER, machine.state());

        machine.onEntered();
        assertEquals(DockState.DOCKED, machine.state());
    }

    @Test
    void dockedTransitionsToExitAfterDuration() {
        machine.requestDock();
        machine.onApproachReached();
        machine.onEntered();
        assertEquals(DockState.DOCKED, machine.state());

        machine.update(1.5f);
        assertEquals(DockState.DOCKED, machine.state(), "still docked before duration elapses");

        machine.update(0.6f);
        assertEquals(DockState.EXIT, machine.state(), "docked duration elapsed");
    }

    @Test
    void exitReturnsToIdle() {
        machine.requestDock();
        machine.onApproachReached();
        machine.onEntered();
        machine.update(10f);
        assertEquals(DockState.EXIT, machine.state());

        machine.onExited();
        assertEquals(DockState.IDLE, machine.state());
        assertFalse(machine.isBusy());
    }

    @Test
    void eventsAreIgnoredOutOfSequence() {
        machine.onApproachReached();
        assertEquals(DockState.IDLE, machine.state(), "approach without request is ignored");

        machine.onEntered();
        assertEquals(DockState.IDLE, machine.state(), "enter without approach is ignored");

        machine.onExited();
        assertEquals(DockState.IDLE, machine.state());
    }

    @Test
    void requestDockIgnoredWhileBusy() {
        machine.requestDock();
        machine.onApproachReached();
        machine.requestDock();
        assertEquals(DockState.ENTER, machine.state(), "re-request during a cycle is ignored");
    }

    @Test
    void resetAbortsCycle() {
        machine.requestDock();
        machine.onApproachReached();
        machine.reset();
        assertEquals(DockState.IDLE, machine.state());
    }

    @Test
    void negativeDurationRejected() {
        assertThrows(IllegalArgumentException.class, () -> new DockStateMachine(-1f));
    }
}
