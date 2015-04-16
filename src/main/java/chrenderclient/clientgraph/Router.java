package chrenderclient.clientgraph;

import com.carrotsearch.hppc.IntArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.PriorityQueue;

/**
 * Class that handles routing on PrioResult/CoreGraph combined graphs
 *
 * Created by niklas on 13.04.15.
 */
public class Router {
    private ArrayList<Bundle> bundles;
    private CoreGraph core;

    public Router(CoreGraph core, ArrayList<Bundle> bundles){
        this.core = core;
        this.bundles = bundles;
    }

    private final class PQElement implements Comparable<PQElement>{
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
    public void route(int srcX, int srcY, int trgtX, int trgtY) {
        System.out.println("From ("+srcX+", "+srcY+") to ("+trgtX+", "+trgtY+")");
        FindBundles findBundles = new FindBundles(srcX, srcY, trgtX, trgtY).invoke();
        Bundle srcBundle = findBundles.getSrcBundle();
        Bundle trgtBundle = findBundles.getTrgtBundle();
        int srcId = findBundles.getSrcId();
        int trgtId = findBundles.getTrgtId();

        if(srcBundle == null || trgtBundle == null) {
            System.err.println("Can't find bundles");
            return;
        }

        System.out.println("srcIndex: " + srcId + " trgtIndex: " + trgtId);
        srcX = srcBundle.nodes[srcId].x;
        srcY = srcBundle.nodes[srcId].y;
        trgtX = trgtBundle.nodes[trgtId].x;
        trgtY = trgtBundle.nodes[trgtId].y;
        System.out.println("Actual from ("+srcX+", "+srcY+") to ("+trgtX+", "+trgtY+")");
        // Scan upGraph
        int[] coreFwdDists =  new int[core.getNodeCount()];
        IntArrayList coreFwdSettled = new IntArrayList();
        Arrays.fill(coreFwdDists, Integer.MAX_VALUE);
        int[] upPreds = scanUpGraph(srcBundle, srcId, coreFwdDists, coreFwdSettled);

        // Scan downGraph backwards
        int[] coreBwdDists =  new int[core.getNodeCount()];
        Arrays.fill(coreBwdDists, Integer.MAX_VALUE);
        int[] downPreds = scanDownGraph(trgtBundle, trgtId, coreBwdDists);
        // Merge to find shortest paths below core
        // Dijkstra on core
        System.out.println("Going into CoreDijkstra with "+coreFwdSettled.size()+" settled nodes");
        int bestId = coreDijkstra(coreFwdDists, coreBwdDists, coreFwdSettled);
        // Backtrack
        int currNode = bestId;


    }

    private int coreDijkstra(int[] coreFwdDists, int[] coreBwdDists, IntArrayList coreFwdSettled) {
        int[] corePreds = new int[core.getNodeCount()];
        int bestDist = Integer.MAX_VALUE;
        int bestId = 0;
        PriorityQueue<PQElement> pq = new PriorityQueue<PQElement>();
        for(int i = 0; i <  coreFwdSettled.size(); ++i) {
            int id = coreFwdSettled.get(i);
            int dist = coreFwdDists[id];
            pq.add(new PQElement(id, dist));
        }
        while(!pq.isEmpty()) {
            PQElement minElement = pq.poll();
            // We may add elements more then once but ignore them here, this removes the need for the
            // decrease-key operation
            int currNode = minElement.id;
            System.out.println("CurrNode: "+currNode);
            if(minElement.dist > coreFwdDists[currNode]){
                continue;
            }

            // Settled in backwards sweep is a best cost candidate
            if(coreBwdDists[currNode] < Integer.MAX_VALUE) {
                int dist = coreFwdDists[currNode] + coreBwdDists[currNode];
                System.out.println("Reached core leave node "+currNode+" dist: "+dist+" coreFwdDist: "+coreFwdDists[currNode]+" coreBwdDist: "+coreBwdDists[currNode]);
                if(dist < bestDist){
                    bestId = currNode;
                    bestDist = dist;
                }
                continue;
            }

            for (int outEdgeNum = 0; outEdgeNum < core.getOutEdgeCount(currNode); ++outEdgeNum) {
                int edgeId = core.getOutEdgeId(currNode, outEdgeNum);
                int trgtId = core.getTarget(edgeId);
                int tmpDist = coreFwdDists[currNode] + core.getCost(edgeId);
                if(tmpDist < coreFwdDists[trgtId]) {
                    coreFwdDists[trgtId] = tmpDist;
                    corePreds[trgtId] = edgeId;
                    pq.add(new PQElement(trgtId, tmpDist));
                    System.out.println("Adding " + trgtId + " with " + tmpDist);
                }

            }

        }

        System.out.println("Cost of path is "+bestDist);
        return bestId;
    }

    private int[] scanDownGraph(Bundle trgtBundle, int trgtId, int[] coreDists) {
        int[] downDists = new int[trgtBundle.nodes.length];
        int[] downPreds = new int[trgtBundle.nodes.length];

        Arrays.fill(downDists, Integer.MAX_VALUE);
        int trgtDownIndex = trgtBundle.nodes[trgtId].downIndex;
        downDists[trgtId] = 0;


        for (int downEdgeIndex = trgtDownIndex;downEdgeIndex < trgtBundle.downEdges.length;++downEdgeIndex) {
            Edge e = trgtBundle.downEdges[downEdgeIndex];
            int tmpDist = downDists[e.trgt-trgtBundle.coreSize];
            if(tmpDist == Integer.MAX_VALUE) {
                continue;
            }
            tmpDist += e.cost;
            assert tmpDist >= 0;
            if(e.src >= trgtBundle.coreSize) {
                int src = e.src-trgtBundle.coreSize;
                if(tmpDist < downDists[src]) {
                    downDists[src] = tmpDist;
                    downPreds[src] = downEdgeIndex;
                }
            } else {
                // TODO figure out how to predecessor save edges entering the core
                if(tmpDist < coreDists[e.src]) {
                    coreDists[e.src] = tmpDist;
                    System.out.println("Entered core backward at "+e.src+" with dist "+tmpDist);
                }
            }

        }
        return downPreds;
    }

    private int[] scanUpGraph(Bundle srcBundle, int srcId, int[] coreDists, IntArrayList coreFwdSettled) {
        int[] upDists = new int[srcBundle.nodes.length];
        int[] upPreds = new int[srcBundle.nodes.length];

        Arrays.fill(upDists, Integer.MAX_VALUE);
        int srcUpIndex = srcBundle.nodes[srcId].upIndex;
        assert srcId == srcBundle.upEdges[srcUpIndex].src-srcBundle.coreSize;
        upDists[srcId] = 0;
        for (int upEdgeIndex = srcUpIndex;upEdgeIndex < srcBundle.upEdges.length; ++upEdgeIndex) {
            Edge e = srcBundle.upEdges[upEdgeIndex];
            int tmpDist = upDists[e.src-srcBundle.coreSize];
            if(tmpDist == Integer.MAX_VALUE) {
                continue;
            }
            tmpDist += e.cost;
            assert tmpDist >= 0;
            if(e.trgt >= srcBundle.coreSize) {
                int trgtUp = e.trgt-srcBundle.coreSize;
                if(tmpDist < upDists[trgtUp]) {
                    upDists[trgtUp] = tmpDist;
                    upPreds[trgtUp] = upEdgeIndex;
                }
            } else {
                // TODO figure out how to predecessor save edges entering the core
                if(tmpDist < coreDists[e.trgt]) {
                    if(coreDists[e.trgt] == Integer.MAX_VALUE) {
                        coreFwdSettled.add(e.trgt);
                    }
                    coreDists[e.trgt] = tmpDist;
                }
            }

        }
        return upPreds;
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
                for(int i = 0; i < bundle.nodes.length; ++i) {
                    Node n = bundle.nodes[i];

                    double srcDist = Math.sqrt(Math.pow(n.x-srcX, 2.0)+Math.pow(n.y-srcY, 2.0));
                    if(srcDist < bestSrcDist) {
                        if(srcBundle != bundle){
                            srcBundle = bundle;
                        }
                        srcId = i;
                        bestSrcDist = srcDist;
                    }

                    double trgtDist = Math.sqrt(Math.pow(n.x-trgtX, 2.0)+Math.pow(n.y-trgtY, 2.0));
                    if(trgtDist < bestTrgtDist) {
                        if(trgtBundle != bundle){
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
