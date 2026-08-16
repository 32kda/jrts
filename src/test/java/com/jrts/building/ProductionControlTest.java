package com.jrts.building;

import com.jrts.movement.NavigationService;
import com.jrts.unit.Unit;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProductionControlTest {

    private Building building;
    private UnitSpawner spawner;
    private NavigationService navigation;
    private Node spawnPoint;
    private Node exitPoint;
    private ProductionControl control;

    @BeforeEach
    void setUp() {
        building = mock(Building.class);
        spawner = mock(UnitSpawner.class);
        navigation = mock(NavigationService.class);

        spawnPoint = new Node("SpawnPoint");
        spawnPoint.setLocalTranslation(0f, 0f, 0f);
        exitPoint = new Node("ExitPoint");
        exitPoint.setLocalTranslation(5f, 0f, 0f);

        when(building.getSpawnPoint()).thenReturn(spawnPoint);
        when(building.getExitPoint()).thenReturn(exitPoint);
        when(building.getBodyYaw()).thenReturn(0f);

        control = new ProductionControl(building, spawner, navigation);
    }

    @Test
    void spawnsUnitAtSpawnPointAndPathsToExit() {
        Unit unit = mock(Unit.class);
        Vector3f spawnPos = new Vector3f(0f, 0f, 0f);
        Vector3f exitPos = new Vector3f(5f, 0f, 0f);
        List<Vector3f> path = List.of(spawnPos, exitPos);

        when(spawner.spawn("heavy_tank", spawnPos, 0f)).thenReturn(unit);
        when(navigation.computePath(spawnPos, exitPos)).thenReturn(path);

        control.enqueue("heavy_tank");
        assertEquals(1, control.queueSize());
        assertTrue(control.isProducing());

        control.processQueue();

        assertEquals(0, control.queueSize());
        assertFalse(control.isProducing());
        verify(spawner).spawn("heavy_tank", spawnPos, 0f);
        verify(unit).setWaypoints(path);
    }

    @Test
    void doesNothingWhenQueueEmpty() {
        control.processQueue();
        verifyNoInteractions(spawner, navigation);
    }

    @Test
    void clearsQueueWhenNoSpawnPoint() {
        when(building.getSpawnPoint()).thenReturn(null);
        control.enqueue("heavy_tank");
        control.processQueue();
        verifyNoInteractions(spawner);
        assertEquals(0, control.queueSize());
    }

    @Test
    void skipsExitPathWhenSpawnFails() {
        when(spawner.spawn(anyString(), any(), anyFloat())).thenReturn(null);
        control.enqueue("heavy_tank");
        control.processQueue();
        verify(navigation, never()).computePath(any(), any());
        assertEquals(0, control.queueSize());
    }
}
