package chrenderclient.clientgraph;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntIndexedContainer;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

/**
 * Holds and magaes access to the data necessary to draw graphs,
 *
 * Created by niklas on 15.06.15.
 */
public class DrawData {
    private final IntArrayList data;
    private static final int DRAW_RECORD_SIZE = 5;
    private BoundingBox bbox;

    public  DrawData() {
        this.data = new IntArrayList();
        this.bbox = new BoundingBox(0, 0 , 0, 0);
    }

    public int size() {
        return data.size()/DRAW_RECORD_SIZE;
    }

    public int getX1(int index) {
        return data.get(index*DRAW_RECORD_SIZE);
    }

    public int getY1(int index) {
        return data.get(index*DRAW_RECORD_SIZE+1);
    }

    public int getX2(int index) {
        return data.get(index*DRAW_RECORD_SIZE+2);
    }

    public int getY2(int index) {
        return data.get(index*DRAW_RECORD_SIZE+3);
    }

    public int getType(int index) {
        return data.get(index*DRAW_RECORD_SIZE+4);
    }

    public BoundingBox getBbox() {
        return bbox;
    }

    private void setBbox(BoundingBox bbox) {
        this.bbox = bbox;
    }

    public void addLine(int srcX, int srcY, int trgtX, int trgtY, int type){
        data.add(srcX, srcY, trgtX, trgtY, type);
        bbox.expandIfNeeded(srcX, srcY);
        bbox.expandIfNeeded(trgtX, trgtY);
    }

    public void addFromIndexed(DrawData other, IntIndexedContainer drawIndices) {
        for (int i = 0; i < drawIndices.size(); ++i) {
            int drawIndex =  drawIndices.get(i);
            addLine(other.getX1(drawIndex), other.getY1(drawIndex), other.getX2(drawIndex), other.getY2(drawIndex), other.getType(drawIndex));
        }
    }

    public static DrawData readDrawData(JsonParser jp, JsonToken token) throws IOException {
        DrawData res = new DrawData();
        // Should be on START_ARRAY
        if (token != JsonToken.START_ARRAY) {
            throw new JsonParseException("draw is not an array", jp.getCurrentLocation());
        }
        while (jp.nextToken() != JsonToken.END_ARRAY) {
            int srcX = jp.getIntValue();
            int srcY = jp.nextIntValue(0);
            int trgtX = jp.nextIntValue(0);
            int trgtY = jp.nextIntValue(0);

            // No nextFloatValue ?
            int type = jp.nextIntValue(0);
            // TODO proper type
            res.addLine(srcX, srcY, trgtX, trgtY, type);
        }
        return res;
    }
}
