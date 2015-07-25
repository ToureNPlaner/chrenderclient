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
    private final IntArrayList vertexData;
    private final IntArrayList edgeData;
    private static final int EDGE_RECORD_SIZE = 3;
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

    public BoundingBox getBbox() {
        return bbox;
    }

    private void addVertex(int x, int y){
        vertexData.add(x, y);
        bbox.expandIfNeeded(x, y);
    }



    /**
     * Add indexed data from other draw, note that this doesn't reuse vertices with the same
     * coordinates currently it's only for generating simple DrawData e.g. for paths
     * @param draw
     * @param path
     */
    public final void addFromIndexed(DrawData draw, IntArrayList path) {
        for (int i = 0; i < path.size(); ++i) {
            int edgeId = path.get(i);
            int x1 = draw.getX1(edgeId);
            int y1 = draw.getY1(edgeId);
            int x2 = draw.getX2(edgeId);
            int y2 = draw.getY2(edgeId);
            int type = draw.getType(edgeId);
            addVertex(x1, y1);
            addVertex(x2, y2);
            edgeData.add(vertexData.size()/VERTEX_RECORD_SIZE - 2, vertexData.size()/VERTEX_RECORD_SIZE - 1, type);
        }
    }

    private void addEdge(int srcId, int trgtId, int type){
        edgeData.add(srcId, trgtId, type);
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

            } else if ("edges".equals(fieldName)) {
                // Should be on START_ARRAY
                if (token != JsonToken.START_ARRAY) {
                    throw new JsonParseException("path is no array", jp.getCurrentLocation());
                }
                while (jp.nextToken() != JsonToken.END_ARRAY) {
                    res.addEdge(jp.getIntValue(), jp.nextIntValue(0), jp.nextIntValue(0));
                }
            }
        }
        return res;
    }

}
