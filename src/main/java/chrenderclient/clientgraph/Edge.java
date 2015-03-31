package chrenderclient.clientgraph;

/**
 * Created by niklas on 31.03.15.
 */
public final class Edge {
    public int src;
    public int trgt;
    public int cost;
    public RefinedPath path;

    public Edge() {path = new RefinedPath();}
}
