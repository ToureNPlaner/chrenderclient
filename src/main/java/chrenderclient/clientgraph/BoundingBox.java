package chrenderclient.clientgraph;

/**
 * Created by niklas on 14.05.15.
 */
public class BoundingBox {
    // Coordinates of upper left corner
    public final int x;
    public final int y;
    public final int width;
    public final int height;

    public BoundingBox(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean contains(int pX, int pY) {
        return (pX > x && pX < x+width) && (pY > y && pY < y+height);
    }
}
