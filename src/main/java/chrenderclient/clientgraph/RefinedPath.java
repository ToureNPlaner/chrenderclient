package chrenderclient.clientgraph;

import com.carrotsearch.hppc.FloatArrayList;
import com.carrotsearch.hppc.IntArrayList;

/**
 * Created by niklas on 31.03.15.
 */
public class RefinedPath {
    private final IntArrayList x1;
    private final IntArrayList y1;
    private final IntArrayList x2;
    private final IntArrayList y2;
    private final FloatArrayList type;

    public RefinedPath() {
        x1 = new IntArrayList();
        y1 = new IntArrayList();
        x2 = new IntArrayList();
        y2 = new IntArrayList();
        type = new FloatArrayList();
    }

    public int size() {
        return x1.size();
    }
    public int getX1(int pos) {
        return x1.get(pos);
    }

    public int getY1(int pos) {
        return y1.get(pos);
    }

    public int getX2(int pos) {
        return x2.get(pos);
    }

    public int getY2(int pos) {
        return y2.get(pos);
    }

    public void setX1(int pos, int val) {
        x1.set(pos, val);
    }

    public void setY1(int pos, int val) {
        y1.set(pos, val);
    }

    public void setX2(int pos, int val) {
        x2.set(pos, val);
    }

    public void setY2(int pos, int val) {
        y2.set(pos, val);
    }

    public float getType(int pos) {
        return type.get(pos);
    }

    public void add(int x1, int y1, int x2, int y2, float type) {
        this.x1.add(x1);
        this.y1.add(y1);
        this.x2.add(x2);
        this.y2.add(y2);
        this.type.add(type);
    }
}
