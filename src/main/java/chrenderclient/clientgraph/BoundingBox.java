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

    /**
     * Constructs a BoundingBox using the given data. Height and width need to be >= 0
     * @param x
     * @param y
     * @param width
     * @param height
     */
    public BoundingBox(int x, int y, int width, int height) {
        assert width >= 0 && height >=0;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Checks whether the BoundingBox has zero area
     * @return
     */
    public boolean isEmpty(){
        return width == 0 || height == 0;
    }

    /**
     * Constructs and empty BoundingBox that can be expanded
     */
    public BoundingBox() {
        this.x = Integer.MAX_VALUE;
        this.y = Integer.MAX_VALUE;
        this.width = 0;
        this.height= 0;
    }

    public boolean contains(int px, int py) {
        long right = x+width;
        long top = y+height;
        return (px >= x && px <= right) && (py >= y && py <= top);
    }

    public void expandIfNeeded(int px, int py){
        if (x == Integer.MAX_VALUE && y == Integer.MAX_VALUE){
            x = px;
            y = py;
            return;
        }

        if(px < x){
            width += x-px;
            x = px;
        } else if (px > x+width) {
            width = px-x;
        }

        if(py < y){
            height += y-py;
            y = py;
        } else if(py > y+height){
            height = py-y;
        }
    }

    public BoundingBox intersect(BoundingBox other) {
        int xnew = Math.max(x, other.x);
        int ynew  = Math.max(y, other.y);
        int rightnew = Math.min(x + width, other.x + other.width);
        int topnew = Math.min(y + height, other.y + other.height);
        return new BoundingBox(xnew, ynew, Math.max(rightnew -  xnew, 0), Math.max(topnew - ynew, 0));
    }

    @Override
    public String toString(){
        return "( "+x+" , "+y+" , "+width+" , "+height+" )";
    }
}
