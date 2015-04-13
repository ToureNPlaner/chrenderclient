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
        FindBundles findBundles = new FindBundles(srcX, srcY, trgtX, trgtY).invoke();
        Bundle srcBundle = findBundles.getSrcBundle();
        Bundle trgtBundle = findBundles.getTrgtBundle();
        int srcUpIndex = findBundles.getSrcUpIndex();
        int trgtDownIndex = findBundles.getTrgtDownIndex();

        if(srcBundle == null || trgtBundle == null) {
            System.err.println("Can't find bundles");
            return;
        }

        System.out.println("srcIndex: " + srcUpIndex + " trgtIndex: " + trgtDownIndex);
        // Scan upGraph
        int[] coreFwdDists =  new int[core.getNodeCount()];
        IntArrayList coreFwdSettled = new IntArrayList();
        Arrays.fill(coreFwdDists, Integer.MAX_VALUE);
        int[] upPreds = scanUpGraph(srcBundle, srcUpIndex, coreFwdDists, coreFwdSettled);

        // Scan downGraph backwards
        int[] coreBwdDists =  new int[core.getNodeCount()];
        Arrays.fill(coreBwdDists, Integer.MAX_VALUE);
        int[] downPreds = scanDownGraph(trgtBundle, trgtDownIndex, coreBwdDists);
        // Merge to find shortest paths below core
        // Dijkstra on core
        int bestId = coreDijkstra(coreFwdDists, coreBwdDists, coreFwdSettled);
        // Backtrack
        int currNode = bestId;


    }

    private int coreDijkstra(int[] coreFwdDists, int[] coreBwdDists, IntArrayList coreFwdSettled) {
        int[] corePreds = new int[core.getNodeCount()];
        int bestDist = Integer.MAX_VALUE;
        int bestId = 0;
        PriorityQueue<PQElement> pq = new PriorityQueue<PQElement>(coreFwdSettled.size());
        for(int i = 0; i <  coreFwdSettled.size(); ++i) {
            int id = coreFwdSettled.get(i);
            pq.add(new PQElement(id, coreFwdDists[id]));
        }
        while(!pq.isEmpty()) {
            PQElement minElement = pq.poll();
            // We may add elements more then once but ignore them here, this removes the need for the
            // decrease-key operation
            if(coreFwdDists[minElement.id] > minElement.dist){
                continue;
            }

            int currNode = minElement.id;
            // Settled in backwards sweep is a best cost candidate
            if(coreBwdDists[currNode] < Integer.MAX_VALUE) {
                int cost = coreFwdDists[currNode] + coreBwdDists[currNode];
                if(cost < bestDist){
                    bestId = currNode;
                    bestDist = cost;
                }
                continue;
            }

            for (int outEdgeNum = 0; outEdgeNum < core.getOutEdgeCount(currNode); ++outEdgeNum) {
                int edgeId = core.getOutEdgeId(currNode, outEdgeNum);
                int trgtId = core.getTarget(edgeId);
                int tmpDist = coreFwdDists[currNode] + core.getCost(edgeId);
                if(tmpDist < coreFwdDists[trgtId]) {
                    coreFwdDists[trgtId] = tmpDist;
                    pq.add(new PQElement(trgtId, tmpDist));
                    corePreds[trgtId] = edgeId;
                }

            }

        }

        System.out.println("Cost of path is "+bestDist);
        return bestId;
    }

    private int[] scanDownGraph(Bundle trgtBundle, int trgtDownIndex, int[] coreDists) {
        int[] downDists = new int[trgtBundle.nodes.length];
        int[] downPreds = new int[trgtBundle.nodes.length];

        Arrays.fill(downDists, Integer.MAX_VALUE);
        downDists[trgtDownIndex] = 0;

        for (int downEdgeIndex = trgtDownIndex;downEdgeIndex < trgtBundle.downEdges.length;++downEdgeIndex) {
            Edge e = trgtBundle.downEdges[downEdgeIndex];
            int tmpDist = downDists[e.trgt] + e.cost;
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
                }
            }

        }
        return downPreds;
    }

    private int[] scanUpGraph(Bundle srcBundle, int srcUpIndex, int[] coreDists, IntArrayList coreFwdSettled) {
        int[] upDists = new int[srcBundle.nodes.length];
        int[] upPreds = new int[srcBundle.nodes.length];

        Arrays.fill(upDists, Integer.MAX_VALUE);
        upDists[srcUpIndex] = 0;

        for (int upEdgeIndex = srcUpIndex;upEdgeIndex < srcBundle.upEdges.length;++upEdgeIndex) {
            Edge e = srcBundle.upEdges[upEdgeIndex];
            int tmpDist = upDists[e.src] + e.cost;
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
        private int srcUpIndex;
        private Bundle trgtBundle;
        private int trgtDownIndex;

        public FindBundles(int srcX, int srcY, int trgtX, int trgtY) {
            this.srcX = srcX;
            this.srcY = srcY;
            this.trgtX = trgtX;
            this.trgtY = trgtY;
        }

        public Bundle getSrcBundle() {
            return srcBundle;
        }

        public int getSrcUpIndex() {
            return srcUpIndex;
        }

        public Bundle getTrgtBundle() {
            return trgtBundle;
        }

        public int getTrgtDownIndex() {
            return trgtDownIndex;
        }

        public FindBundles invoke() {
            srcBundle = null;
            srcUpIndex = 0;
            trgtBundle = null;
            trgtDownIndex = 0;

            // TODO linear search sucks
            int bestSrcDist = Integer.MAX_VALUE;
            int bestTrgtDist = Integer.MAX_VALUE;
            for (Bundle bundle : bundles) {
                for(int i = 0; i < bundle.nodes.length; ++i) {
                    Node n = bundle.nodes[i];
                    int srcDist = (n.x-srcX)*(n.x-srcX)+(n.y-srcY)*(n.y-srcY);
                    int trgtDist = (n.x-trgtX)*(n.x-trgtX)+(n.y-trgtY)*(n.y-trgtY);
                    if(srcDist < bestSrcDist) {
                        if(srcBundle != bundle){
                            srcBundle = bundle;
                        }
                        srcUpIndex = srcBundle.nodes[i].upIndex;
                        bestSrcDist = srcDist;
                    }

                    if(trgtDist < bestTrgtDist) {
                        if(trgtBundle != bundle){
                            trgtBundle = bundle;
                        }
                        trgtDownIndex = trgtBundle.nodes[i].downIndex;
                        bestSrcDist = srcDist;
                    }
                }
            }
            return this;
        }
    }
}
