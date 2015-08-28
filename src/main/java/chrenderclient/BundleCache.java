package chrenderclient;

import chrenderclient.clientgraph.BoundingBox;
import chrenderclient.clientgraph.Bundle;

/**
 * The Bundle Cache caches a constant number of bundles evicting
 * bundles by an implementation defined strategy. Bundles are stored
 * in arbitrary order and this order may change with offer
 * operations as bundles are evicted.
 */
public final class BundleCache implements Iterable<Bundle> {
    public final class Iterator implements java.util.Iterator<Bundle> {
        private Iterator() {
            this.index = -1;

        }

        /**
         * Returns {@code true} if the iteration has more elements.
         * (In other words, returns {@code true} if {@link #next} would
         * return an element rather than throwing an exception.)
         *
         * @return {@code true} if the iteration has more elements
         */
        @Override
        public boolean hasNext() {
            return index < n-1;
        }

        /**
         * Returns the next element in the iteration.
         *
         * @return the next element in the iteration
         */
        @Override
        public Bundle next() {
            return bundles[++index];
        }

        private int index;
    }


    public BundleCache(int cacheSize, double nonOverlapThreshold, int minEdgeCount){
        if(cacheSize < 1){
            throw new IllegalArgumentException("Caches need to have size at least 1");
        }
        bundles = new Bundle[cacheSize];
        n = 0;
        insertIndex = 0;
        this.nonOverlapThreshold = nonOverlapThreshold;
        this.minEdgeCount = minEdgeCount;
    }

    /*
    Rates how much use we get out of the old bundle once the fresh has been added,
    we will evict the bundle with least use
     */
    private double rateDifference(Bundle fresh, Bundle old){
        // We get more use of a bundle the less area it shares with the new one
        BoundingBox freshBBox = fresh.requestParams.bbox;
        BoundingBox oldBBox = old.requestParams.bbox;
        BoundingBox intersection = oldBBox.intersect(freshBBox);
        if(intersection.isEmpty()){
            return 1.0;
        }
        double oldArea = (double)oldBBox.width*(double)oldBBox.height;
        double freshArea = (double)freshBBox.width*(double)freshBBox.height;
        double intersectArea = (double)intersection.width*(double)intersection.height;
        double diffArea = 1.0 - intersectArea/(Math.max(oldArea, freshArea));
        System.out.println("diffArea: "+diffArea);

        return diffArea;
    }

    public void offer(Bundle bundle) {
        // We throw away empty bundles
        if (bundle.nodes.length < 1) {
            return;
        }
        // First element initializes the hot seat
        if(n == 0){
            n = 1;
            bundles[0] = bundle;
            return;
        }

        if(bundle.downEdges.length + bundle.upEdges.length > minEdgeCount) {
            double minDiff = Double.MAX_VALUE;
            // Position 0 is the hot seat all others are organized as a ring buffer
            for (int i = 1; i < n; i++) {
                double diff = rateDifference(bundle, bundles[i]);
                if (diff < minDiff) {
                    minDiff = diff;
                }
            }

            if (minDiff > nonOverlapThreshold) {
                // Cache the bundle
                if (n < bundles.length) {
                    n++;
                }
                // We skip the hot seat at pos=0
                bundles[insertIndex + 1] = bundle;
                insertIndex = (insertIndex + 1) % (bundles.length - 1);
                return;
            }
        }

        bundles[0] = bundle;
        return;
    }

    public int maxSize() {
        return bundles.length;
    }

    public int size() {
        return n;
    }

    public boolean isEmpty(){
        return n <= 0;
    }

    public Iterator iterator() {
        return new Iterator();
    }

    private int n;
    /*
    We use a ring buffer intenally and insertIndex always points to the position where
    we insert so either an empty position or the oldest element.
     */
    private int insertIndex;
    private final double nonOverlapThreshold;
    private final int minEdgeCount;
    private final Bundle[] bundles;
}
