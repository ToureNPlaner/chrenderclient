package chrenderclient.clientgraph;

import com.carrotsearch.hppc.IntArrayList;
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
    private IntArrayList vertexData;
    private final IntArrayList edgeData;
    private static final int EDGE_RECORD_SIZE = 5;
    private static final int VERTEX_RECORD_SIZE = 2;
    private BoundingBox bbox;

    public  DrawData() {
        this.vertexData = new IntArrayList();
        this.edgeData = new IntArrayList();
        this.bbox = new BoundingBox();
    }

    public int size() {
        return edgeData.size()/EDGE_RECORD_SIZE;
    }

    public int numVertices() {
        return vertexData.size()/VERTEX_RECORD_SIZE;
    }

    public int getX(int vertexId) {
        return vertexData.get(vertexId*VERTEX_RECORD_SIZE);
    }

    public int getY(int vertexId) {
        return vertexData.get(vertexId*VERTEX_RECORD_SIZE+1);
    }

    public int getX1(int edgeId) {
        return vertexData.get(getSource(edgeId)*VERTEX_RECORD_SIZE);
    }

    public int getY1(int edgeId) {
        return vertexData.get(getSource(edgeId)*VERTEX_RECORD_SIZE+1);
    }

    public int getX2(int edgeId) {
        return vertexData.get(getTarget(edgeId)*VERTEX_RECORD_SIZE);
    }

    public int getY2(int edgeId) {
        return vertexData.get(getTarget(edgeId)*VERTEX_RECORD_SIZE+1);
    }

    public int getSource(int edgeId) {return edgeData.get(edgeId*EDGE_RECORD_SIZE);}

    public int getTarget(int edgeId) {return edgeData.get(edgeId*EDGE_RECORD_SIZE+1);}

    public int getType(int edgeId) {
        return edgeData.get(edgeId*EDGE_RECORD_SIZE+2);
    }

    public int getDrawScA(int edgeId) {return edgeData.get(edgeId*EDGE_RECORD_SIZE+3);}

    public int getDrawScB(int edgeId) {
        return edgeData.get(edgeId*EDGE_RECORD_SIZE+4);
    }

    public BoundingBox getBbox() {
        return bbox;
    }

    /**
     * Adds a vertex to this draw and returns its new vertexId
     * @param x
     * @param y
     * @return
     */
    protected final int addVertex(int x, int y){
        vertexData.add(x, y);
        bbox.expandIfNeeded(x, y);
        return numVertices()-1;
    }

    /**
     * Add the edge to this draw and return its new index
     * @param srcId
     * @param trgtId
     * @param type
     * @param drawScA
     * @param drawScB
     * @return
     */
    protected int addEdge(int srcId, int trgtId, int type, int drawScA, int drawScB){
        edgeData.add(srcId, trgtId, type, drawScA, drawScB);
        return this.size() - 1;
    }

    public static DrawData readJson(JsonParser jp, JsonToken token) throws IOException {
        DrawData res = new DrawData();
        // Should be on START_OBJECT
        if (token != JsonToken.START_OBJECT) {
            throw new JsonParseException("draw is not an object", jp.getCurrentLocation());
        }
        String fieldName;
        int minX = Integer.MAX_VALUE;
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            fieldName = jp.getCurrentName();
            token = jp.nextToken();
            if ("vertices".equals(fieldName)) {
                // Should be on START_ARRAY
                if (token != JsonToken.START_ARRAY) {
                    throw new JsonParseException("path is no array", jp.getCurrentLocation());
                }
                while (jp.nextToken() != JsonToken.END_ARRAY) {
                    int x = jp.getIntValue();
                    int y = jp.nextIntValue(0);
                    res.addVertex(x, y);
                }

            } else if ("lines".equals(fieldName)) {
                // Should be on START_ARRAY
                if (token != JsonToken.START_ARRAY) {
                    throw new JsonParseException("lines is no array", jp.getCurrentLocation());
                }
                while (jp.nextToken() != JsonToken.END_ARRAY) {
                    int srcId = jp.getIntValue();
                    int trgtId = jp.nextIntValue(0);
                    int type = jp.nextIntValue(0);
                    int drawScA = jp.nextIntValue(0);
                    int drawScB = jp.nextIntValue(0);
                    res.addEdge(srcId, trgtId, type, drawScA, drawScB);
                }
            }
        }
        return res;
    }

}
