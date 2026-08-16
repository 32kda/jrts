package com.jrts.pathfinding;

import com.jrts.util.OctileDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Hierarchical pathfinding A* (HPA*, Botea et al. 2004) over a {@link Grid}.
 *
 * Amortizes the cost of pathfinding on large grids: a coarse abstract graph over cluster
 * entrances is searched first, and the result is refined into a concrete cell path. Intra-cluster
 * shortest paths are computed lazily (and cached) with the fine-grained {@link AStarPathfinder}.
 *
 * The result is a valid, optimal path with the same cost as a full fine-grained A* on a uniform
 * grid, but with far fewer cells expanded for large maps.
 */
public class HpaPathfinder implements Pathfinder {

    private static final Logger log = LoggerFactory.getLogger(HpaPathfinder.class);

    private static final float ORTHO = 10f;
    private static final float DIAG = 14f;
    private static final float INF = Float.POSITIVE_INFINITY;

    private final int clusterSize;
    private final AStarPathfinder fine = new AStarPathfinder();

    private HpaGraph graph;
    private Grid cachedGrid;
    private TraversalProfile cachedProfile;
    private final Map<Long, HpaGraph.Edge> intraCache = new HashMap<>();

    public HpaPathfinder() {
        this(8);
    }

    public HpaPathfinder(int clusterSize) {
        this.clusterSize = clusterSize;
    }

    @Override
    public PathResult findPath(Grid grid, GridCell start, GridCell goal, TraversalProfile profile) {
        if (!grid.isTraversable(start, profile)) {
            return PathResult.failure(PathResult.START_BLOCKED);
        }
        if (!grid.hasClearance(goal, profile)) {
            return PathResult.failure(PathResult.GOAL_BLOCKED);
        }
        if (start.equals(goal)) {
            return PathResult.success(List.of(start));
        }

        HpaGraph g = getGraph(grid, profile);

        if (g.clusterIndexOf(start) == g.clusterIndexOf(goal)) {
            return fine.findPath(grid, start, goal, profile);
        }

        return abstractSearch(g, start, goal, profile);
    }

    /**
     * Drop the cached abstract graph and intra-cluster paths. Call after mutating grid
     * walkability (dynamic obstacles).
     */
    public void clearCache() {
        graph = null;
        cachedGrid = null;
        cachedProfile = null;
        intraCache.clear();
    }

    private HpaGraph getGraph(Grid grid, TraversalProfile profile) {
        if (graph == null || cachedGrid != grid || !cachedProfile.equals(profile)) {
            graph = new HpaGraph(grid, profile, clusterSize);
            cachedGrid = grid;
            cachedProfile = profile;
            intraCache.clear();
            log.debug("Built HPA graph: {} entrances on {}x{} grid",
                    graph.nodeCount(), grid.getWidth(), grid.getHeight());
        }
        return graph;
    }

    private PathResult abstractSearch(HpaGraph g, GridCell start, GridCell goal, TraversalProfile profile) {
        int n = g.nodeCount();
        int virtualStart = n;
        int virtualGoal = n + 1;
        int total = n + 2;

        Map<Integer, HpaGraph.Edge> startEdges = startEdges(g, start, profile);
        Map<Integer, HpaGraph.Edge> goalEdges = goalEdges(g, goal, profile);

        float[] gScore = new float[total];
        Arrays.fill(gScore, INF);
        boolean[] closed = new boolean[total];
        int[] cameFrom = new int[total];
        Arrays.fill(cameFrom, -1);
        HpaGraph.Edge[] cameEdge = new HpaGraph.Edge[total];
        float[] h = heuristic(g, start, goal, n);

        gScore[virtualStart] = 0f;
        PriorityQueue<Long> open = new PriorityQueue<>();
        open.add(pack(gScore[virtualStart] + h[virtualStart], virtualStart));

        while (!open.isEmpty()) {
            int current = unpackIndex(open.poll());
            if (closed[current]) {
                continue;
            }
            if (current == virtualGoal) {
                return PathResult.success(reconstruct(cameFrom, cameEdge, virtualStart, virtualGoal, start));
            }
            closed[current] = true;

            List<HpaGraph.Edge> neighbors = neighbors(g, current, startEdges, goalEdges, profile);
            for (HpaGraph.Edge edge : neighbors) {
                int next = edge.to();
                if (closed[next] || edge.cost() >= INF) {
                    continue;
                }
                float tentative = gScore[current] + edge.cost();
                if (tentative < gScore[next]) {
                    gScore[next] = tentative;
                    cameFrom[next] = current;
                    cameEdge[next] = edge;
                    open.add(pack(tentative + h[next], next));
                }
            }
        }

        return PathResult.failure(PathResult.UNREACHABLE);
    }

    private List<HpaGraph.Edge> neighbors(HpaGraph g, int node,
                                          Map<Integer, HpaGraph.Edge> startEdges,
                                          Map<Integer, HpaGraph.Edge> goalEdges,
                                          TraversalProfile profile) {
        int n = g.nodeCount();
        if (node == n) {
            return new ArrayList<>(startEdges.values());
        }
        if (node == n + 1) {
            return List.of();
        }

        List<HpaGraph.Edge> edges = new ArrayList<>(g.interEdges(node));

        GridCell cell = g.node(node);
        int cluster = g.clusterIndexOf(cell);
        for (int other : g.entrancesOfCluster(cluster)) {
            if (other != node) {
                edges.add(intraEdge(g, node, other, profile));
            }
        }
        HpaGraph.Edge toGoal = goalEdges.get(node);
        if (toGoal != null) {
            edges.add(toGoal);
        }
        return edges;
    }

    private Map<Integer, HpaGraph.Edge> startEdges(HpaGraph g, GridCell start, TraversalProfile profile) {
        Map<Integer, HpaGraph.Edge> result = new HashMap<>();
        int cluster = g.clusterIndexOf(start);
        int[] bounds = g.clusterBoundsOf(cluster);
        for (int entrance : g.entrancesOfCluster(cluster)) {
            PathResult r = fine.findPath(g.grid(), start, g.node(entrance), profile,
                    bounds[0], bounds[1], bounds[2], bounds[3]);
            if (r.found()) {
                result.put(entrance, new HpaGraph.Edge(entrance, pathCost(g.grid(), r.cells(), profile), r.cells()));
            }
        }
        return result;
    }

    private Map<Integer, HpaGraph.Edge> goalEdges(HpaGraph g, GridCell goal, TraversalProfile profile) {
        Map<Integer, HpaGraph.Edge> result = new HashMap<>();
        int cluster = g.clusterIndexOf(goal);
        int[] bounds = g.clusterBoundsOf(cluster);
        for (int entrance : g.entrancesOfCluster(cluster)) {
            PathResult r = fine.findPath(g.grid(), goal, g.node(entrance), profile,
                    bounds[0], bounds[1], bounds[2], bounds[3]);
            if (r.found()) {
                List<GridCell> reversed = new ArrayList<>(r.cells());
                Collections.reverse(reversed);
                result.put(entrance, new HpaGraph.Edge(g.nodeCount() + 1,
                        pathCost(g.grid(), r.cells(), profile), reversed));
            }
        }
        return result;
    }

    private HpaGraph.Edge intraEdge(HpaGraph g, int from, int to, TraversalProfile profile) {
        long key = ((long) from << 32) | (to & 0xFFFFFFFFL);
        HpaGraph.Edge cached = intraCache.get(key);
        if (cached != null) {
            return cached;
        }

        GridCell fromCell = g.node(from);
        int[] bounds = g.clusterBoundsOf(g.clusterIndexOf(fromCell));
        PathResult r = fine.findPath(g.grid(), fromCell, g.node(to), profile,
                bounds[0], bounds[1], bounds[2], bounds[3]);

        HpaGraph.Edge edge = r.found()
                ? new HpaGraph.Edge(to, pathCost(g.grid(), r.cells(), profile), r.cells())
                : new HpaGraph.Edge(to, INF, List.of());
        intraCache.put(key, edge);
        return edge;
    }

    private float[] heuristic(HpaGraph g, GridCell start, GridCell goal, int n) {
        float[] h = new float[n + 2];
        for (int i = 0; i < n; i++) {
            GridCell cell = g.node(i);
            h[i] = OctileDistance.distance(cell.x(), cell.z(), goal.x(), goal.z()) * ORTHO;
        }
        h[n] = OctileDistance.distance(start.x(), start.z(), goal.x(), goal.z()) * ORTHO;
        h[n + 1] = 0f;
        return h;
    }

    private List<GridCell> reconstruct(int[] cameFrom, HpaGraph.Edge[] cameEdge,
                                       int virtualStart, int virtualGoal, GridCell start) {
        List<HpaGraph.Edge> edges = new ArrayList<>();
        int cur = virtualGoal;
        while (cur != virtualStart) {
            edges.add(cameEdge[cur]);
            cur = cameFrom[cur];
        }
        Collections.reverse(edges);

        List<GridCell> concrete = new ArrayList<>();
        concrete.add(start);
        for (HpaGraph.Edge edge : edges) {
            List<GridCell> cells = edge.path();
            for (int i = 1; i < cells.size(); i++) {
                concrete.add(cells.get(i));
            }
        }
        return concrete;
    }

    private float pathCost(Grid grid, List<GridCell> cells, TraversalProfile profile) {
        float cost = 0f;
        for (int i = 1; i < cells.size(); i++) {
            GridCell a = cells.get(i - 1);
            GridCell b = cells.get(i);
            boolean diagonal = a.x() != b.x() && a.z() != b.z();
            cost += (diagonal ? DIAG : ORTHO) * grid.getCost(b);
        }
        return cost;
    }

    private static long pack(float f, int index) {
        return ((long) Float.floatToIntBits(f) << 32) | (index & 0xFFFFFFFFL);
    }

    private static int unpackIndex(long packed) {
        return (int) (packed & 0xFFFFFFFFL);
    }
}
