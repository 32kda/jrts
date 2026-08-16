package com.jrts.pathfinding;

import com.jrts.config.AbilitiesSection;
import com.jrts.config.AudioSection;
import com.jrts.config.BuildingInteractionsSection;
import com.jrts.config.CombatSection;
import com.jrts.config.IdentitySection;
import com.jrts.config.MovementSection;
import com.jrts.config.PassengersSection;
import com.jrts.config.SpecialFlagsSection;
import com.jrts.config.StatsSection;
import com.jrts.config.TurretsSection;
import com.jrts.config.UnitConfig;
import com.jrts.config.VeterancySection;
import com.jrts.unit.Unit;
import com.jrts.unit.UnitFlags;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

/**
 * Shared test helpers for the pathfinding module: ASCII grid fixtures and unit building.
 */
public final class PathfindingTestSupport {

    private PathfindingTestSupport() {
    }

    /** Result of parsing an ASCII map: a grid plus the located start/goal cells. */
    public record GridMap(Grid grid, GridCell start, GridCell goal) {
    }

    /**
     * Parse an ASCII map into a {@link Grid}. Top row is the highest Z (north),
     * left column is the lowest X (west).
     *
     * Legend: '#' = blocked, '.' = free, 'S' = start, 'G' = goal.
     */
    public static GridMap parseGrid(String... rows) {
        int height = rows.length;
        int width = rows[0].length();
        Grid grid = new Grid(0f, 0f, width, height, 1f);
        GridCell start = null;
        GridCell goal = null;
        for (int z = 0; z < height; z++) {
            String row = rows[height - 1 - z];
            for (int x = 0; x < width; x++) {
                char c = row.charAt(x);
                if (c == '#') {
                    grid.setBlocked(new GridCell(x, z), true);
                } else if (c == 'S') {
                    start = new GridCell(x, z);
                } else if (c == 'G') {
                    goal = new GridCell(x, z);
                }
            }
        }
        return new GridMap(grid, start, goal);
    }

    /**
     * Build a movable, selectable, turreted test unit at the given position.
     */
    public static Unit createUnit(int id, float x, float y, float z) {
        UnitConfig config = createConfig();
        int flags = UnitFlags.fromUnitConfig(config);
        Node spatial = new Node("TestUnit" + id);
        Unit unit = new Unit(id, config, spatial, flags);
        unit.setPosition(new Vector3f(x, y, z));
        unit.setBodyYaw(0f);
        return unit;
    }

    private static UnitConfig createConfig() {
        return new UnitConfig(
                new IdentitySection("Test", "Test", "AFV", "Republic",
                        false, false, true, true, true, false, false, false, "unit"),
                new StatsSection(100, "light", 5, 8f, 5f, 5f, 100, 10, 1, -1, 10f),
                null, 0,
                new VeterancySection(false, null, null),
                new CombatSection("", "", "", "", -1, 0f, false, 64, false, false, false),
                new AbilitiesSection(
                        new AbilitiesSection.DeploySection(false, 0f, null),
                        new AbilitiesSection.DockingAbilitySection(false, "", 0f, 0)),
                new PassengersSection(0),
                new AudioSection(null, null, null, null, null, null, ""),
                new MovementSection("tracks", false, true, false),
                new TurretsSection(false, false, null, 0f, null, 0f,
                        false, 0f, 0f, 0f, 0f, 0f, 5f, 2f),
                new BuildingInteractionsSection("", "", false, false, false),
                new SpecialFlagsSection(false));
    }
}
