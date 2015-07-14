package chrenderclient.clientgraph;

import com.carrotsearch.hppc.IntArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * Class that handles routing on PrioResult/CoreGraph combined graphs
 * <p/>
 * Created by niklas on 13.04.15.
 */
public class Router {
    private ArrayList<Bundle> bundles;
    private CoreGraph core;

    private int srcId;
    private int trgtId;
    private Bundle srcBundle;
    private Bundle trgtBundle;
    private int bestDist;
    private DrawData bestPath;
    private int[] coreEnterPreds;
    private int[] coreLeavePreds;
    private int[] coreFwdDists;
    private int[] coreBwdDists;
    private int[] corePreds;


    public Router(CoreGraph core, ArrayList<Bundle> bundles) {
        this.core = core;
        this.bundles = bundles;
        coreEnterPreds = new int[core.getNodeCount()];
        coreLeavePreds = new int[core.getNodeCount()];
        corePreds = new int[core.getNodeCount()];
        coreFwdDists = new int[core.getNodeCount()];
        coreBwdDists = new int[core.getNodeCount()];
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
    public DrawData route(int srcX, int srcY, int trgtX, int trgtY) {
        System.out.println("From (" + srcX + ", " + srcY + ") to (" + trgtX + ", " + trgtY + ")");
        long start = System.nanoTime();
        // TODO src/trgt node in core
        this.bestDist = Integer.MAX_VALUE;
        this.bestPath = null;
        findSourceAndTarget(srcX, srcY, trgtX, trgtY);
        System.out.println(Utils.took("finding ids", start));
        System.out.println("srcId: "+srcId+((srcBundle != null)?" (bundle)":"(core)")+" trgtId: "+trgtId+((trgtBundle != null)?" (bundle)":"(core)"));
        // Scan upGraph
        int[] upDists = null;
        int[] upPreds = null;
        IntArrayList coreFwdSettled = new IntArrayList();
        Arrays.fill(coreEnterPreds, -1);
        Arrays.fill(coreFwdDists, Integer.MAX_VALUE);
        if(srcBundle != null) {
            upDists = new int[srcBundle.nodes.length];
            upPreds = scanUpGraph(srcBundle, srcId, upDists, coreFwdDists, coreEnterPreds, coreFwdSettled);
            System.out.println(Utils.took("scanUpGraph", start));
        } else {
            coreFwdSettled.add(srcId);
            coreFwdDists[srcId] = 0;
            coreEnterPreds[srcId] = 0;
        }
        start = System.nanoTime();

        // Scan downGraph backwards
        int[] downDists = null;
        int[] downPreds = null;
        IntArrayList coreBwdSettled = new IntArrayList();
        Arrays.fill(coreBwdDists, Integer.MAX_VALUE);
        if(trgtBundle != null) {
            downDists = new int[trgtBundle.nodes.length];
            downPreds = scanDownGraph(trgtBundle, trgtId, downDists, coreBwdDists, coreLeavePreds, coreBwdSettled);
            System.out.println(Utils.took("scanDownGraph", start));
        } else {
            coreBwdSettled.add(trgtId);
            coreBwdDists[trgtId] = 0;
        }
        start = System.nanoTime();

        if(srcBundle != null && trgtBundle != null) {
            tryMergeBelowCore(srcBundle, trgtBundle, srcId, trgtId, upPreds, downPreds, upDists, downDists);
            System.out.println(Utils.took("Merge below core", start));
            start = System.nanoTime();
            boolean done = bestDist < Integer.MAX_VALUE && checkBestPath(coreFwdSettled, coreBwdSettled);
            System.out.println(Utils.took("checkBestPath", start));
            if (done) {
                return bestPath;
            }
        }

        // Dijkstra on core
        System.out.println("Going into CoreDijkstra with " + coreFwdSettled.size() + " settled nodes");
        start = System.nanoTime();
        int bestId = coreDijkstra(coreFwdDists, coreBwdDists, corePreds, coreFwdSettled);
        System.out.println(Utils.took("coreDijkstra", start));
        if (bestId < 0) {
            return bestPath;
        }

        // Backtrack
        start = System.nanoTime();
        backtrackWithCore(srcBundle, trgtBundle, upPreds, downPreds, corePreds, coreEnterPreds, coreLeavePreds, srcId, trgtId, bestId);
        System.out.println(Utils.took("backtrackWithCore", start));
        return bestPath;
    }

    private void findSourceAndTarget(int srcX, int srcY, int trgtX, int trgtY) {
        srcBundle = null;
        trgtBundle = null;
        srcId = 0;
        trgtId = 0;

        double bestSrcDist = Double.MAX_VALUE;
        double bestTrgtDist = Double.MAX_VALUE;
        for (int nodeId = 0; nodeId < core.getNodeCount(); ++nodeId) {
            int nX = core.getX(nodeId);
            int nY = core.getY(nodeId);
            double srcDist = Math.hypot(nX - srcX, nY - srcY);
            if (srcDist < bestSrcDist) {
                srcId = nodeId;
                bestSrcDist = srcDist;
            }

            double trgtDist = Math.hypot(nX - trgtX, nY - trgtY);
            if (trgtDist < bestTrgtDist) {
                trgtId = nodeId;
                bestTrgtDist = trgtDist;
            }
        }


        //Predicate<Bundle> containsSourceAndTarget = b -> b.getBbox().contains(srcX, srcY) && b.getBbox().contains(trgtX, trgtY);
        //Iterator<Bundle> it = bundles.parallelStream().filter(containsSourceAndTarget).iterator();
        Iterator<Bundle> it  = bundles.iterator();
        while (it.hasNext()) {
            Bundle bundle = it.next();
            for (int i = 0; i < bundle.nodes.length; ++i) {
                Node n = bundle.nodes[i];
                if(!n.hasCoordinates()){
                    continue;
                }

                double srcDist = Math.hypot(n.getX() - srcX, n.getY() - srcY);
                if (srcDist < bestSrcDist) {
                    srcBundle = bundle;
                    srcId = i;
                    bestSrcDist = srcDist;
                }

                double trgtDist = Math.hypot(n.getX() - trgtX, n.getY() - trgtY);
                if (trgtDist < bestTrgtDist) {
                    trgtBundle = bundle;
                    trgtId = i;
                    bestTrgtDist = trgtDist;
                }
            }
        }
    }

    private boolean checkBestPath(IntArrayList fwdSettled, IntArrayList bwdSettled) {
        // Find minimum fwd settled
        int minFwd = Integer.MAX_VALUE;
        for (int i = 0; i < fwdSettled.size(); ++i) {
            int val = fwdSettled.get(i);
            if (coreFwdDists[val] < minFwd) {
                minFwd = coreFwdDists[val];
            }
        }

        // Find minimum bwd settled
        int minBwd = Integer.MAX_VALUE;
        for (int i = 0; i < bwdSettled.size(); ++i) {
            int val = bwdSettled.get(i);
            if (coreBwdDists[val] < minBwd) {
                minBwd = coreBwdDists[val];
            }
        }

        return (minFwd + minBwd) >= bestDist;
    }

    private void tryMergeBelowCore(Bundle srcBundle, Bundle trgtBundle, int srcId, int trgtId, int[] upPreds, int[] downPreds, int[] upDists, int[] downDists) {
        int bestDownId = -1;
        int bestUpId = -1;
        int mergeBestDist = Integer.MAX_VALUE;
        DrawData path = null;

        int i = 0;
        int j = 0;
        while (i < srcBundle.nodes.length || j < trgtBundle.nodes.length) {
            if (srcBundle.nodes[i].getOriginalId() == trgtBundle.nodes[j].getOriginalId()) {
                if (upDists[i] < Integer.MAX_VALUE && downDists[j] < Integer.MAX_VALUE) {
                    int tmpDist = upDists[i] + downDists[j];
                    if (tmpDist < mergeBestDist) {
                        mergeBestDist = tmpDist;
                        bestUpId = i;
                        bestDownId = j;
                    }
                }
                ++i;
                ++j;
            } else if (srcBundle.nodes[i].getOriginalId() < trgtBundle.nodes[j].getOriginalId()) {
                ++i;
            } else if (srcBundle.nodes[i].getOriginalId() > trgtBundle.nodes[j].getOriginalId()) {
                ++j;
            }
        }
        if (mergeBestDist < Integer.MAX_VALUE) {
            System.out.println("Best Dist below core: " + mergeBestDist + " bestUpId: " + bestUpId + " bestDownId: " + bestDownId);
            path = new DrawData();
            // up graph
            int currNode = bestUpId;
            while (currNode != srcId) {
                int upEdgeIndex = upPreds[currNode];
                Edge edge = srcBundle.upEdges[upEdgeIndex];
                // if order mattered added at start
                path.addFromIndexed(srcBundle.getDraw(), edge.path);
                currNode = edge.src - srcBundle.coreSize;
            }

            // down graph
            currNode = bestDownId;
            while (currNode != trgtId) {
                int downEdgeIndex = downPreds[currNode];
                Edge currEdge = trgtBundle.downEdges[downEdgeIndex];
                // if order mattered added  at end
                path.addFromIndexed(trgtBundle.getDraw(), currEdge.path);
                currNode = currEdge.trgt - trgtBundle.coreSize;
            }
        }
        this.bestDist = mergeBestDist;
        this.bestPath = path;
        return;
    }

    private void backtrackWithCore(Bundle srcBundle, Bundle trgtBundle, int[] upPreds, int[] downPreds, int[] corePreds, int[] coreEnterPreds, int[] coreLeavePreds, int srcId, int trgtId, int bestId) {
        // bestId is in core, we need to backtrack trough the core and separately through the up- and
        // down-Graphs. bestId is also at the edge of the core leaving to the down graph
        DrawData path = new DrawData();

        // down graph
        int currNode;
        if(trgtBundle != null) {
            int downEdgeIndex = coreLeavePreds[bestId];
            Edge currEdge = trgtBundle.downEdges[downEdgeIndex];
            currNode = currEdge.trgt - trgtBundle.coreSize;
            // if order mattered added at end
            path.addFromIndexed(trgtBundle.getDraw(), currEdge.path);
            while (currNode != trgtId) {
                downEdgeIndex = downPreds[currNode];
                currEdge = trgtBundle.downEdges[downEdgeIndex];
                // if order mattered added at end
                path.addFromIndexed(trgtBundle.getDraw(), currEdge.path);
                currNode = currEdge.trgt - trgtBundle.coreSize;
            }
        }


        // core graph
        currNode = bestId;
        while (coreEnterPreds[currNode] == -1) {
            int currEdgeId = corePreds[currNode];
            // if order mattered added at start
            path.addFromIndexed(core.getDraw(), core.getPath(currEdgeId));
            currNode = core.getSource(currEdgeId);
        }

        // up graph
        if(srcBundle != null) {
            int upEdgeIndex = coreEnterPreds[currNode];
            Edge edge = srcBundle.upEdges[upEdgeIndex];
            // if order mattered added at start
            path.addFromIndexed(srcBundle.getDraw(), edge.path);
            currNode = edge.src - srcBundle.coreSize;
            while (currNode != srcId) {
                upEdgeIndex = upPreds[currNode];
                edge = srcBundle.upEdges[upEdgeIndex];
                // if order mattered added at start
                path.addFromIndexed(srcBundle.getDraw(), edge.path);
                currNode = edge.src - srcBundle.coreSize;
            }
        }
        bestPath = path;
        return;
    }

    private int coreDijkstra(int[] coreFwdDists, int[] coreBwdDists, int[] corePreds, IntArrayList coreFwdSettled) {
        int dijkstraBestDist = Integer.MAX_VALUE;
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

            // if the minimum is already higher cost than the best known path we can't find anything better anymore
            if (minElement.dist > bestDist) {
                System.out.println("Aborting core Dijkstra at dist " + minElement.dist);
                return -1;
            }

            // Settled in backwards sweep is a best cost candidate
            if (coreBwdDists[currNode] < Integer.MAX_VALUE) {
                int dist = coreFwdDists[currNode] + coreBwdDists[currNode];
                System.out.println("Reached core leave node " + currNode + " dist: " + dist + " coreFwdDist: " + coreFwdDists[currNode] + " coreBwdDist: " + coreBwdDists[currNode]);
                if (dist < dijkstraBestDist) {
                    bestId = currNode;
                    dijkstraBestDist = dist;
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

        System.out.println("Cost of dijkstra path is " + dijkstraBestDist);
        if (dijkstraBestDist > bestDist) {
            return -1;
        }
        return bestId;
    }

    private int[] scanUpGraph(Bundle srcBundle, int srcId, int[] upDists, int[] coreDists, int[] coreEnterPreds, IntArrayList coreFwdSettled) {
        int[] upPreds = new int[srcBundle.nodes.length];

        Arrays.fill(upDists, Integer.MAX_VALUE);
        int srcUpIndex = srcBundle.nodes[srcId].getUpIndex();
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
                    assert (e.src - srcBundle.coreSize) < trgtUp; // topo sort property
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

    private int[] scanDownGraph(Bundle trgtBundle, int trgtId, int[] downDists, int[] coreDists, int[] coreLeavePreds, IntArrayList coreBwdSettled) {
        int[] downPreds = new int[trgtBundle.nodes.length];

        Arrays.fill(downDists, Integer.MAX_VALUE);
        int trgtDownIndex = trgtBundle.nodes[trgtId].getDownIndex();
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
                    assert (e.trgt - trgtBundle.coreSize) < srcDown; // topo sort property
                    if (tmpDist < downDists[srcDown]) {
                        downDists[srcDown] = tmpDist;
                        downPreds[srcDown] = downEdgeIndex;
                    }
                } else {
                    System.out.println("Entering (backward) core at " + e.src);
                    // Edges leaving the core are saved
                    if (tmpDist < coreDists[e.src]) {
                        if (coreDists[e.src] == Integer.MAX_VALUE) {
                            coreBwdSettled.add(e.src);
                        }
                        coreDists[e.src] = tmpDist;
                        coreLeavePreds[e.src] = downEdgeIndex;
                    }
                }
            }

        }
        return downPreds;
    }
}
