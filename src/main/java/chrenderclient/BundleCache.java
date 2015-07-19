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


    public BundleCache(int cacheSize){
        bundles = new Bundle[cacheSize];
        n = 0;
    }

    /*
    Rates how much use we get out of the old bundle once the fresh has been added,
    we will evict the bundle with least use
     */
    private double rateUse(Bundle fresh, Bundle old){
        // We get more use of a bundle the less area it shares with the new one
        BoundingBox freshBBox = fresh.getBbox();
        BoundingBox oldBBox = old.getBbox();
        BoundingBox intersection = oldBBox.intersect(freshBBox);
        double useArea = oldBBox.width*oldBBox.height-intersection.width*intersection.height;
        return useArea;
    }

    public void offer(Bundle bundle){
        if(n < bundles.length){
            bundles[n++] = bundle;
            return;
        }
        // Find a bundle to evict and replace
        double minUse = 0;
        int replace = 0;
        for (int i = 0; i < bundles.length; i++) {
            double use = rateUse(bundles[i], bundle);
            if(use <= minUse){
                minUse = use;
                replace = i;
            }
        }
        bundles[replace] = bundle;
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
    private final Bundle[] bundles;
}
