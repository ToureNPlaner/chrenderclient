package chrenderclient.clientgraph;

/**
 * Created by niklas on 06.05.15.
 */
public class Utils {
    public static final String took(String what, long start) {
        long now = System.nanoTime();
        return "TIMING: "+what+" took "+ nsToMs(now - start) + " ms ";
    }

    public static final double nsToMs(long timespan){
        return ((double)timespan)/1000000.0;
    }

    public static String sizeForHumans(long size) {
        double s =((double) size)/((double) (2<<20));
        return String.format("%.2f MiB", s);
    }
}
