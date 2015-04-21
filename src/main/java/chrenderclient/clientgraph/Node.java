package chrenderclient.clientgraph;

/**
 * Created by niklas on 09.04.15.
 */
public final class Node {
    public int x;
    public int y;
    public int oId;
    protected int upIndex;
    protected int downIndex;

    public Node(int x, int y, int originalId) {
        this.x = x;
        this.y = y;
        this.oId = originalId;
    }
}
