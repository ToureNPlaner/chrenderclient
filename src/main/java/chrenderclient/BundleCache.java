package chrenderclient;

import chrenderclient.clientgraph.BoundingBox;
import chrenderclient.clientgraph.Bundle;

/**
 * The Bundle Cache caches a constant number of bundles evicting
 * bundles by an implementation defined strategy. Bundles are stored
 * in arbitrary order and this order may change with offer
 * operations as bundles are evicted.
 */
public class BundleCache implements Iterable<Bundle> {
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


    public BundleCache(int cacheSize, double threshold, int minEdgeCount){
        bundles = new Bundle[cacheSize];
        n = 0;
        this.overlapThreshold = threshold;
        this.minEdgeCount = minEdgeCount;
    }

    /*
    Rates how much use we get out of the old bundle once the fresh has been added,
    we will evict the bundle with least use
     */
    private double rateDifference(Bundle fresh, Bundle old){
        // We get more use of a bundle the less area it shares with the new one
        BoundingBox freshBBox = fresh.bbox;
        BoundingBox oldBBox = old.bbox;
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

    public void offer(Bundle bundle){
        // Don't care about empty bundles
        if(bundle.bbox.isEmpty()){
            return;
        }

        // Warm up phase
        if(n < bundles.length){
            if(n <= 1){
                // The first two we simply add, unless they are empty
                bundles[n] = bundle;
                n++;
            } else {
                // After we replace the hot element (the zeroth)
                // or grow if all of the older ones is are at least 75% different
                int insert = 0;
                double minDiff = 1.0;
                for(int i = 1; i < n; i++){
                    double diff = rateDifference(bundle, bundles[i]);
                    if(diff <= minDiff){
                        minDiff = diff;
                    }
                }
                if(minDiff > overlapThreshold && bundle.getDraw().size() > minEdgeCount){
                    insert = n;
                    n++;
                    System.out.println("Adding bundle");
                }
                bundles[insert] = bundle;
            }
            return;
        }

        // We keep the zeroth element hot and all others at least threshold different
        int minIndex = 0;
        double minDiff = 1.0;
        for(int i = 1; i < n; i++){
            double diff = rateDifference(bundle, bundles[i]);
            if(diff <= minDiff){
                minDiff = diff;
                minIndex = i;
            }
        }
        if(minDiff > overlapThreshold && bundle.getDraw().size() > minEdgeCount) {
            bundles[minIndex] = bundle;
            System.out.println("Replacing old bundle");
            return;
        }
        bundles[0] = bundle;
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
    private final double overlapThreshold;
    private final int minEdgeCount;
    private final Bundle[] bundles;
}
