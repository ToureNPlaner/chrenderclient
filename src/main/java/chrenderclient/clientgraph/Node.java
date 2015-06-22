package chrenderclient.clientgraph;

/**
 * A Node in a Bundle, note that nodes outside the visible bounding box may not have
 * coordinates
 *
 * Created by niklas on 09.04.15.
 */
public final class Node {
    private final int x;
    private final int y;
    private final int oId;
    private int upIndex;
    private int downIndex;

    /**
     * Constructs a Node with coordinates
     *
     * @param x
     * @param y
     * @param originalId
     */
    public Node(int x, int y, int originalId) {
        this.x = x;
        this.y = y;
        this.oId = originalId;
    }

    /**
     *  Constructs a Node without coordinates that is outside the visible bounding box.
     *  These must be ignored during nearest neighbor search using hasCoordinates()
     */
    public Node(int originalId) {
        this.x = Integer.MAX_VALUE;
        this.y = Integer.MAX_VALUE;
        this.oId = originalId;
    }

    public boolean hasCoordinates() {
        return x != Integer.MAX_VALUE && y != Integer.MAX_VALUE;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getOriginalId() {
        return oId;
    }

    public int getUpIndex() {
        return upIndex;
    }

    public void setUpIndex(int upIndex) {
        this.upIndex = upIndex;
    }

    public int getDownIndex() {
        return downIndex;
    }

    public void setDownIndex(int downIndex) {
        this.downIndex = downIndex;
    }
}
