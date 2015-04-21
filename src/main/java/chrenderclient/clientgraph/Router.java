package chrenderclient.clientgraph;

import com.carrotsearch.hppc.IntArrayList;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.PriorityQueue;

/**
 * Class that handles routing on PrioResult/CoreGraph combined graphs
 * <p/>
 * Created by niklas on 13.04.15.
 */
public class Router {
    private ArrayList<Bundle> bundles;
    private CoreGraph core;

    public Router(CoreGraph core, ArrayList<Bundle> bundles) {
        this.core = core;
        this.bundles = bundles;
    }

    private final class PQElement implements Comparable<PQElement> {
        public int id;
        public int dist;

        public PQElement(int id, int dist) {
            this.id = id;
            this.dist = dist;
        }

        /**
         * @see Comparable
         */
        @Override
        public int compareTo(PQElement o) {
            return this.dist - o.dist;
        }
    }

    /*
    Compute route from (srcX, srcY) to (trgtX, trgtY), for now we change the type
    of the path segments to show the path, we will do this right in the future (TODO)
     */
    public ArrayDeque<RefinedPath> route(int srcX, int srcY, int trgtX, int trgtY) throws PathNotFoundException {
        System.out.println("From (" + srcX + ", " + srcY + ") to (" + trgtX + ", " + trgtY + ")");
        FindBundles findBundles = new FindBundles(srcX, srcY, trgtX, trgtY).invoke();
        Bundle srcBundle = findBundles.getSrcBundle();
        Bundle trgtBundle = findBundles.getTrgtBundle();
        int srcId = findBundles.getSrcId();
        int trgtId = findBundles.getTrgtId();

        if (srcBundle == null || trgtBundle == null) {
            System.err.println("Can't find bundles");
            throw new PathNotFoundException("Can't find bundle");
        }

        System.out.println("srcIndex: " + srcId + " trgtIndex: " + trgtId);
        srcX = srcBundle.nodes[srcId].x;
        srcY = srcBundle.nodes[srcId].y;
        trgtX = trgtBundle.nodes[trgtId].x;
        trgtY = trgtBundle.nodes[trgtId].y;
        System.out.println("Actual from (" + srcX + ", " + srcY + ") to (" + trgtX + ", " + trgtY + ")");
        // Scan upGraph
        int[] coreEnterPreds = new int[core.getNodeCount()];
        Arrays.fill(coreEnterPreds, -1);
        int[] coreFwdDists = new int[core.getNodeCount()];
        int[] upDists = new int[srcBundle.nodes.length];
        IntArrayList coreFwdSettled = new IntArrayList();
        Arrays.fill(coreFwdDists, Integer.MAX_VALUE);
        int[] upPreds = scanUpGraph(srcBundle, srcId, upDists, coreFwdDists, coreEnterPreds, coreFwdSettled);

        // Scan downGraph backwards
        int[] coreLeavePreds = new int[core.getNodeCount()];
        int[] coreBwdDists = new int[core.getNodeCount()];
        int[] downDists = new int[trgtBundle.nodes.length];
        Arrays.fill(coreBwdDists, Integer.MAX_VALUE);
        int[] downPreds = scanDownGraph(trgtBundle, trgtId, downDists, coreBwdDists, coreLeavePreds);


        // Merge to find shortest paths below core
        ArrayDeque<RefinedPath> path = tryMergeBelowCore(srcBundle, trgtBundle, srcId, trgtId, upPreds, downPreds, upDists, downDists);
        if (path != null) {
            return path;
        }

        // Dijkstra on core
        System.out.println("Going into CoreDijkstra with " + coreFwdSettled.size() + " settled nodes");
        int[] corePreds = new int[core.getNodeCount()];
        int bestId = coreDijkstra(coreFwdDists, coreBwdDists, corePreds, coreFwdSettled);
        if (bestId < 0) {
            throw new PathNotFoundException("Can't reach target");
        }
        // Backtrack
         path = backtrackWithCore(srcBundle, trgtBundle, upPreds, downPreds, corePreds, coreEnterPreds, coreLeavePreds, srcId, trgtId, bestId);
        return path;
    }

    private ArrayDeque<RefinedPath> tryMergeBelowCore(Bundle srcBundle, Bundle trgtBundle, int srcId, int trgtId, int[] upPreds, int[] downPreds, int[] upDists, int[] downDists) {
        int bestDownId = -1;
        int bestUpId = -1;
        int bestDist = Integer.MAX_VALUE;
        ArrayDeque<RefinedPath> path = null;

        int i = 0;
        int j = 0;
        while ( i < srcBundle.nodes.length || j < trgtBundle.nodes.length) {
            if (srcBundle.nodes[i].oId == trgtBundle.nodes[j].oId) {
                if(upDists[i] < Integer.MAX_VALUE && downDists[j] < Integer.MAX_VALUE) {
                    int tmpDist = upDists[i] + downDists[j];
                    if (tmpDist < bestDist) {
                        bestDist = tmpDist;
                        bestUpId = i;
                        bestDownId = j;
                    }
                }
                ++i;
                ++j;
            } else if (srcBundle.nodes[i].oId < trgtBundle.nodes[j].oId) {
                ++i;
            } else if (srcBundle.nodes[i].oId > trgtBundle.nodes[j].oId) {
                ++j;
            }
        }
        if (bestDist < Integer.MAX_VALUE) {
            System.out.println("Best Dist below core: " + bestDist + " bestUpId: " + bestUpId + " bestDownId: " + bestDownId);
            path = new ArrayDeque<RefinedPath>();
            // up graph
            int currNode = bestUpId;
            while (currNode != srcId) {
                int upEdgeIndex = upPreds[currNode];
                Edge edge = srcBundle.upEdges[upEdgeIndex];
                path.addFirst(edge.path);
                currNode = edge.src - srcBundle.coreSize;
            }

            // down graph
            currNode = bestDownId;
            while (currNode != trgtId) {
                int downEdgeIndex = downPreds[currNode];
                Edge currEdge = trgtBundle.downEdges[downEdgeIndex];
                path.addLast(currEdge.path);
                currNode = currEdge.trgt - trgtBundle.coreSize;
            }
        }
        return path;
    }

    private ArrayDeque<RefinedPath> backtrackWithCore(Bundle srcBundle, Bundle trgtBundle, int[] upPreds, int[] downPreds, int[] corePreds, int[] coreEnterPreds, int[] coreLeavePreds, int srcId, int trgtId, int bestId) {
        // bestId is in core, we need to backtrack trough the core and separately through the up- and
        // down-Graphs. bestId is also at the edge of the core leaving to the down graph
        ArrayDeque<RefinedPath> path = new ArrayDeque<RefinedPath>();

        // down graph
        int downEdgeIndex = coreLeavePreds[bestId];
        Edge currEdge = trgtBundle.downEdges[downEdgeIndex];
        int currNode = currEdge.trgt - trgtBundle.coreSize;
        path.addLast(currEdge.path);
        while (currNode != trgtId) {
            downEdgeIndex = downPreds[currNode];
            currEdge = trgtBundle.downEdges[downEdgeIndex];
            path.addLast(currEdge.path);
            currNode = currEdge.trgt - trgtBundle.coreSize;
        }


        // core graph
        currNode = bestId;
        while (coreEnterPreds[currNode] == -1) {
            int currEdgeId = corePreds[currNode];
            path.addFirst(core.getRefinedPath(currEdgeId));
            currNode = core.getSource(currEdgeId);
        }

        // up graph
        int upEdgeIndex = coreEnterPreds[currNode];
        Edge edge = srcBundle.upEdges[upEdgeIndex];
        path.addFirst(edge.path);
        currNode = edge.src - srcBundle.coreSize;
        while (currNode != srcId) {
            upEdgeIndex = upPreds[currNode];
            edge = srcBundle.upEdges[upEdgeIndex];
            path.addFirst(edge.path);
            currNode = edge.src - srcBundle.coreSize;
        }

        return path;
    }

    private int coreDijkstra(int[] coreFwdDists, int[] coreBwdDists, int[] corePreds, IntArrayList coreFwdSettled) {
        int bestDist = Integer.MAX_VALUE;
        int bestId = -1;
        PriorityQueue<PQElement> pq = new PriorityQueue<PQElement>();
        for (int i = 0; i < coreFwdSettled.size(); ++i) {
            int id = coreFwdSettled.get(i);
            int dist = coreFwdDists[id];
            pq.add(new PQElement(id, dist));
        }
        while (!pq.isEmpty()) {
            PQElement minElement = pq.poll();
            // We may add elements more then once but ignore them here, this removes the need for the
            // decrease-key operation
            int currNode = minElement.id;
            if (minElement.dist > coreFwdDists[currNode]) {
                continue;
            }

            // Settled in backwards sweep is a best cost candidate
            if (coreBwdDists[currNode] < Integer.MAX_VALUE) {
                int dist = coreFwdDists[currNode] + coreBwdDists[currNode];
                System.out.println("Reached core leave node " + currNode + " dist: " + dist + " coreFwdDist: " + coreFwdDists[currNode] + " coreBwdDist: " + coreBwdDists[currNode]);
                if (dist < bestDist) {
                    bestId = currNode;
                    bestDist = dist;
                }
                continue;
            }

            for (int outEdgeNum = 0; outEdgeNum < core.getOutEdgeCount(currNode); ++outEdgeNum) {
                int edgeId = core.getOutEdgeId(currNode, outEdgeNum);
                int trgtId = core.getTarget(edgeId);
                int tmpDist = coreFwdDists[currNode] + core.getCost(edgeId);
                if (tmpDist < coreFwdDists[trgtId]) {
                    coreFwdDists[trgtId] = tmpDist;
                    corePreds[trgtId] = edgeId;
                    pq.add(new PQElement(trgtId, tmpDist));
                }

            }

        }

        System.out.println("Cost of path is " + bestDist);
        return bestId;
    }

    private int[] scanUpGraph(Bundle srcBundle, int srcId, int[] upDists,int[] coreDists, int[] coreEnterPreds, IntArrayList coreFwdSettled) {
        int[] upPreds = new int[srcBundle.nodes.length];

        Arrays.fill(upDists, Integer.MAX_VALUE);
        int srcUpIndex = srcBundle.nodes[srcId].upIndex;
        assert srcId == srcBundle.upEdges[srcUpIndex].src - srcBundle.coreSize;
        upDists[srcId] = 0;
        for (int upEdgeIndex = 0; upEdgeIndex < srcBundle.upEdges.length; ++upEdgeIndex) {
            Edge e = srcBundle.upEdges[upEdgeIndex];
            int tmpDist = upDists[e.src - srcBundle.coreSize];
            if (tmpDist < Integer.MAX_VALUE) {

                tmpDist += e.cost;
                assert tmpDist >= 0;
                if (e.trgt >= srcBundle.coreSize) {
                    int trgtUp = e.trgt - srcBundle.coreSize;
                    assert (e.src-srcBundle.coreSize) < trgtUp; // topo sort property
                    if (tmpDist < upDists[trgtUp]) {
                        upDists[trgtUp] = tmpDist;
                        upPreds[trgtUp] = upEdgeIndex;
                    }
                } else {
                    // Edges entering the core are saved
                    System.out.println("Entering core at " + e.trgt);
                    if (tmpDist < coreDists[e.trgt]) {
                        if (coreDists[e.trgt] == Integer.MAX_VALUE) {
                            coreFwdSettled.add(e.trgt);
                        }
                        coreDists[e.trgt] = tmpDist;
                        coreEnterPreds[e.trgt] = upEdgeIndex;
                    }
                }
            }

        }
        return upPreds;
    }

    private int[] scanDownGraph(Bundle trgtBundle, int trgtId, int[] downDists, int[] coreDists, int[] coreLeavePreds) {
        int[] downPreds = new int[trgtBundle.nodes.length];

        Arrays.fill(downDists, Integer.MAX_VALUE);
        int trgtDownIndex = trgtBundle.nodes[trgtId].downIndex;
        assert trgtId == trgtBundle.downEdges[trgtDownIndex].trgt - trgtBundle.coreSize;
        downDists[trgtId] = 0;


        for (int downEdgeIndex = 0; downEdgeIndex < trgtBundle.downEdges.length; ++downEdgeIndex) {
            Edge e = trgtBundle.downEdges[downEdgeIndex];

            int tmpDist = downDists[e.trgt - trgtBundle.coreSize];
            if (tmpDist < Integer.MAX_VALUE) {

                tmpDist += e.cost;
                assert tmpDist >= 0;
                if (e.src >= trgtBundle.coreSize) {
                    int srcDown = e.src - trgtBundle.coreSize;
                    assert (e.trgt-trgtBundle.coreSize) < srcDown; // topo sort property
                    if (tmpDist < downDists[srcDown]) {
                        downDists[srcDown] = tmpDist;
                        downPreds[srcDown] = downEdgeIndex;
                    }
                } else {
                    System.out.println("Entering (backward) core at " + e.src);
                    // Edges leaving the core are saved
                    if (tmpDist < coreDists[e.src]) {
                        coreDists[e.src] = tmpDist;
                        coreLeavePreds[e.src] = downEdgeIndex;
                    }
                }
            }

        }
        return downPreds;
    }

    private class FindBundles {
        private int srcX;
        private int srcY;
        private int trgtX;
        private int trgtY;
        private Bundle srcBundle;
        private int srcId;
        private Bundle trgtBundle;
        private int trgtId;

        public FindBundles(int srcX, int srcY, int trgtX, int trgtY) {
            this.srcX = srcX;
            this.srcY = srcY;
            this.trgtX = trgtX;
            this.trgtY = trgtY;
        }

        public Bundle getSrcBundle() {
            return srcBundle;
        }

        public int getSrcId() {
            return srcId;
        }

        public Bundle getTrgtBundle() {
            return trgtBundle;
        }

        public int getTrgtId() {
            return trgtId;
        }

        public FindBundles invoke() {
            srcBundle = null;
            srcId = 0;
            trgtBundle = null;
            trgtId = 0;

            // TODO linear search sucks
            double bestSrcDist = Double.MAX_VALUE;
            double bestTrgtDist = Double.MAX_VALUE;
            for (Bundle bundle : bundles) {
                for (int i = 0; i < bundle.nodes.length; ++i) {
                    Node n = bundle.nodes[i];

                    double srcDist = Math.sqrt(Math.pow(n.x - srcX, 2.0) + Math.pow(n.y - srcY, 2.0));
                    if (srcDist < bestSrcDist) {
                        if (srcBundle != bundle) {
                            srcBundle = bundle;
                        }
                        srcId = i;
                        bestSrcDist = srcDist;
                    }

                    double trgtDist = Math.sqrt(Math.pow(n.x - trgtX, 2.0) + Math.pow(n.y - trgtY, 2.0));
                    if (trgtDist < bestTrgtDist) {
                        if (trgtBundle != bundle) {
                            trgtBundle = bundle;
                        }
                        trgtId = i;
                        bestTrgtDist = trgtDist;
                    }
                }
            }
            return this;
        }
    }
}
