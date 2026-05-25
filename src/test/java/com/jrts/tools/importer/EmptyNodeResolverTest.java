package com.jrts.tools.importer;

import com.jme3.scene.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmptyNodeResolverTest {

    private EmptyNodeResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new EmptyNodeResolver();
    }

    @Test
    void resolveTurretPivotExactMatch() {
        Node root = new Node("Root");
        addNode(root, "TurretPivot");

        ModelManifest manifest = resolver.resolve(root);
        assertTrue(manifest.hasRole(NodeRole.TURRET_PIVOT));
    }

    @Test
    void resolveCaseInsensitive() {
        Node root = new Node("Root");
        addNode(root, "turretpivot");

        ModelManifest manifest = resolver.resolve(root);
        assertTrue(manifest.hasRole(NodeRole.TURRET_PIVOT));
    }

    @Test
    void resolvePrefixedName() {
        Node root = new Node("Root");
        addNode(root, "HeavyTank_TurretPivot");

        ModelManifest manifest = resolver.resolve(root);
        assertTrue(manifest.hasRole(NodeRole.TURRET_PIVOT));
    }

    @Test
    void turretMeshNotConfusedWithTurretPivot() {
        Node root = new Node("Root");
        addNode(root, "Turret");

        ModelManifest manifest = resolver.resolve(root);
        assertFalse(manifest.hasRole(NodeRole.TURRET_PIVOT));
        assertTrue(manifest.hasRole(NodeRole.TURRET_MESH));
    }

    @Test
    void unknownNodeNameReturnsNoRole() {
        Node root = new Node("Root");
        addNode(root, "RandomNode");

        ModelManifest manifest = resolver.resolve(root);
        assertFalse(manifest.hasRole(NodeRole.TURRET_PIVOT));
        assertFalse(manifest.hasRole(NodeRole.CHASSIS));
    }

    @Test
    void resolveChassis() {
        Node root = new Node("Root");
        addNode(root, "Chassis");

        ModelManifest manifest = resolver.resolve(root);
        assertTrue(manifest.hasRole(NodeRole.CHASSIS));
    }

    @Test
    void resolveMuzzle() {
        Node root = new Node("Root");
        addNode(root, "Muzzle");

        ModelManifest manifest = resolver.resolve(root);
        assertTrue(manifest.hasRole(NodeRole.MUZZLE));
    }

    @Test
    void resolveBarrelPivot() {
        Node root = new Node("Root");
        addNode(root, "BarrelPivot");

        ModelManifest manifest = resolver.resolve(root);
        assertTrue(manifest.hasRole(NodeRole.BARREL_PIVOT));
    }

    @Test
    void resolveWheelNodes() {
        Node root = new Node("Root");
        addNode(root, "Wheel_FL");
        addNode(root, "Wheel_FR");
        addNode(root, "Wheel_RL");
        addNode(root, "Wheel_RR");

        ModelManifest manifest = resolver.resolve(root);
        assertTrue(manifest.hasRole(NodeRole.WHEEL_FL));
        assertTrue(manifest.hasRole(NodeRole.WHEEL_FR));
        assertTrue(manifest.hasRole(NodeRole.WHEEL_RL));
        assertTrue(manifest.hasRole(NodeRole.WHEEL_RR));
    }

    @Test
    void resolveMultipleRolesInHierarchy() {
        Node root = new Node("Root");
        Node chassis = addNode(root, "Chassis");
        addNode(chassis, "TurretPivot");

        ModelManifest manifest = resolver.resolve(root);
        assertEquals(2, manifest.getRoles().size());
        assertTrue(manifest.hasRole(NodeRole.CHASSIS));
        assertTrue(manifest.hasRole(NodeRole.TURRET_PIVOT));
    }

    private static Node addNode(Node parent, String name) {
        Node child = new Node(name);
        parent.attachChild(child);
        return child;
    }
}
