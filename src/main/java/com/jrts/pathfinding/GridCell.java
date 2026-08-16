package com.jrts.pathfinding;

/**
 * Immutable 2D grid coordinate. X is the horizontal axis, Z the depth axis
 * (matching the world XZ plane used by pathfinding).
 */
public record GridCell(int x, int z) {
}
