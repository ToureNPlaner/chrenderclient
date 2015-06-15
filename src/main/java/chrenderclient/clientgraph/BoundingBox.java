package chrenderclient.clientgraph;

/**
 * Created by niklas on 14.05.15.
 */
public class BoundingBox {
    // Coordinates of upper left corner
    public int x;
    public int y;
    public int width;
    public int height;

    public BoundingBox(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean contains(int px, int py) {
        long right = x+width;
        long top = y+height;
        return (px >= x && px < right) && (py >= y && py < top);
    }

    public void expandIfNeeded(int px, int py) {
        x = Math.min(x, px);
        y = Math.min(y, py);
        width = Math.max(px-x, width);
        height = Math.max(py-y, height);
    }
}
