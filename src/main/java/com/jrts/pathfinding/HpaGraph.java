package com.jrts.pathfinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The abstract graph used by hierarchical pathfinding (HPA*, Botea et al. 2004).
 *
 * Divides the grid into fixed-size square clusters. An <em>entrance</em> is a traversable
 * cell on a cluster boundary that has a traversable neighbor in an adjacent cluster. The
 * abstract graph connects entrances:
 * <ul>
 *   <li><em>inter-cluster</em> edges between the two cells of a boundary transition (cheap, precomputed)</li>
 *   <li><em>intra-cluster</em> edges between entrances of the same cluster (shortest paths, computed lazily by {@link HpaPathfinder})</li>
 * </ul>
 */
public class HpaGraph {

    /** A directed abstract edge with the concrete cell path from the source to the target. */
    public record Edge(int to, float cost, List<GridCell> path) {
    }

    private static final int[][] DIRS8 = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    private final Grid grid;
    private final TraversalProfile profile;
    private final int clusterSize;
    private final int clustersX;
    private final int clustersZ;

    private final List<GridCell> nodes = new ArrayList<>();
    private final Map<GridCell, Integer> nodeIds = new HashMap<>();
    private final List<List<Edge>> interEdges = new ArrayList<>();
    private final Map<Integer, List<Integer>> entrancesByCluster = new HashMap<>();

    public HpaGraph(Grid grid, TraversalProfile profile, int clusterSize) {
        this.grid = grid;
        this.profile = profile;
        this.clusterSize = clusterSize;
        this.clustersX = (grid.getWidth() + clusterSize - 1) / clusterSize;
        this.clustersZ = (grid.getHeight() + clusterSize - 1) / clusterSize;
        build();
    }

    private void build() {
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int z = 0; z < grid.getHeight(); z++) {
                GridCell cell = new GridCell(x, z);
                if (!grid.hasClearance(cell, profile)) {
                    continue;
                }
                if (hasCrossClusterNeighbor(cell)) {
                    register(cell);
                }
            }
        }

        for (int id = 0; id < nodes.size(); id++) {
            GridCell cell = nodes.get(id);
            List<Edge> edges = new ArrayList<>();
            for (int[] d : DIRS8) {
                GridCell n = new GridCell(cell.x() + d[0], cell.z() + d[1]);
                if (!grid.isInBounds(n)) {
                    continue;
                }
                if (clusterX(cell.x()) == clusterX(n.x()) && clusterZ(cell.z()) == clusterZ(n.z())) {
                    continue;
                }
                Integer nid = nodeIds.get(n);
                if (nid == null) {
                    continue;
                }
                boolean diagonal = cell.x() != n.x() && cell.z() != n.z();
                float step = diagonal ? 14f : 10f;
                edges.add(new Edge(nid, step * grid.getCost(n), List.of(cell, n)));
            }
            interEdges.add(edges);
        }

        for (int id = 0; id < nodes.size(); id++) {
            GridCell cell = nodes.get(id);
            entrancesByCluster
                    .computeIfAbsent(clusterIndexOf(cell.x(), cell.z()), k -> new ArrayList<>())
                    .add(id);
        }
    }

    private void register(GridCell cell) {
        if (!nodeIds.containsKey(cell)) {
            nodeIds.put(cell, nodes.size());
            nodes.add(cell);
        }
    }

    private boolean hasCrossClusterNeighbor(GridCell cell) {
        for (int[] d : DIRS8) {
            GridCell n = new GridCell(cell.x() + d[0], cell.z() + d[1]);
            if (!grid.isInBounds(n)) {
                continue;
            }
            if (clusterX(cell.x()) != clusterX(n.x()) || clusterZ(cell.z()) != clusterZ(n.z())) {
                if (isValidTransition(cell, n)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return true if a unit can move from {@code from} to the adjacent {@code to} cell
     *         (clearance of the destination + no diagonal corner cutting)
     */
    private boolean isValidTransition(GridCell from, GridCell to) {
        if (!grid.hasClearance(to, profile)) {
            return false;
        }
        int dx = to.x() - from.x();
        int dz = to.z() - from.z();
        if (dx != 0 && dz != 0) {
            return grid.isTraversable(new GridCell(from.x() + dx, from.z()), profile)
                    && grid.isTraversable(new GridCell(from.x(), from.z() + dz), profile);
        }
        return true;
    }

    public Grid grid() {
        return grid;
    }

    public TraversalProfile profile() {
        return profile;
    }

    public int clusterSize() {
        return clusterSize;
    }

    public int nodeCount() {
        return nodes.size();
    }

    public GridCell node(int id) {
        return nodes.get(id);
    }

    public List<Edge> interEdges(int id) {
        return interEdges.get(id);
    }

    public List<Integer> entrancesOfCluster(int clusterIndex) {
        return entrancesByCluster.getOrDefault(clusterIndex, List.of());
    }

    private int clusterX(int cellX) {
        return cellX / clusterSize;
    }

    private int clusterZ(int cellZ) {
        return cellZ / clusterSize;
    }

    public int clusterIndexOf(int cellX, int cellZ) {
        return clusterX(cellX) * clustersZ + clusterZ(cellZ);
    }

    public int clusterIndexOf(GridCell cell) {
        return clusterIndexOf(cell.x(), cell.z());
    }

    public int[] clusterBoundsOf(int clusterIndex) {
        int cx = clusterIndex / clustersZ;
        int cz = clusterIndex % clustersZ;
        return new int[]{
                cx * clusterSize,
                cz * clusterSize,
                Math.min(cx * clusterSize + clusterSize - 1, grid.getWidth() - 1),
                Math.min(cz * clusterSize + clusterSize - 1, grid.getHeight() - 1)
        };
    }
}
