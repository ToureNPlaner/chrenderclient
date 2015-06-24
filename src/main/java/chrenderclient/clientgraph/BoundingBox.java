package chrenderclient.clientgraph;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
     * Constructs and empty BoundingBox that can be expanded
     */
    public BoundingBox() {
        this.x = 0;
        this.y = 0;
        this.width = -1;
        this.height= -1;
    }
    @JsonIgnore
    public boolean isValid() {
        return height > 0 && width > 0;
    }

    public boolean contains(int px, int py) {
        long right = x+width;
        long top = y+height;
        return (px >= x && px < right) && (py >= y && py < top);
    }

    public void expandIfNeeded(int px, int py) {
        if(width < 0){
            x = px;
            y = py;
            height = 0;
            width = 0;
        }
        x = Math.min(x, px);
        y = Math.min(y, py);
        width = Math.max(px-x, width);
        height = Math.max(py-y, height);
    }
}
