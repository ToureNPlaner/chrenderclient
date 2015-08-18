package chrenderclient.clientgraph;


import com.carrotsearch.hppc.IntArrayList;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

/**
 * Client side graph data structure for storing the Core, that is
 * all nodes and edges above some specified rank/level
 */
public final class CoreGraph {
    public static final class RequestParams {
        public RequestParams(int nodeCount, int minLen, int maxLen, double maxRatio) {
            this.nodeCount = nodeCount;
            this.minLen = minLen;
            this.maxLen = maxLen;
            this.maxRatio = maxRatio;
        }

        public final int nodeCount;
        public final int minLen;
        public final int maxLen;
        public final double maxRatio;
    }

    public final RequestParams requestParams;

    private int nodeCount;
    private int edgeCount;

    private final DrawData draw;

    private int[] offsetOut;
    private int[] xs;
    private int[] ys;

    private int[] srcs;
    private int[] trgts;
    private int[] costs;
    private IntArrayList[] paths;

    // Debugging
    public long requestSize;

    private CoreGraph(RequestParams requestParams, int nodeCount, int edgeCount, DrawData draw) {
        this.requestParams = requestParams;
        this.nodeCount = nodeCount;
        this.edgeCount = edgeCount;

        this.xs = new int[nodeCount];
        this.ys = new int[nodeCount];

        srcs = new int[edgeCount];
        trgts = new int[edgeCount];
        costs = new int[edgeCount];
        paths = new IntArrayList[edgeCount];
        this.draw = draw;
    }

    private void generateOffsets() {
        this.offsetOut = new int[nodeCount + 1];

        for (int i = 0; i < edgeCount; ++i) {
            offsetOut[srcs[i]]++;
        }
        int outSum = 0;
        for (int i = 0; i < nodeCount; ++i) {
            int oldOutSum = outSum;
            outSum += offsetOut[i];
            offsetOut[i] = oldOutSum;
        }
        offsetOut[nodeCount] = outSum;
    }

    private void setEdge(int pos, int src, int trgt, int cost, IntArrayList path) {
        srcs[pos] = src;
        trgts[pos] = trgt;
        costs[pos] = cost;
        paths[pos] = path;
    }

    public int getSource(int edgeId) {
        return srcs[edgeId];
    }

    public int getTarget(int edgeId) {
        return trgts[edgeId];
    }

    public int getCost(int edgeId) {
        return costs[edgeId];
    }

    public IntArrayList getPath(int edgeId) {
        return paths[edgeId];
    }

    public int getOutEdgeCount(int nodeId) {
        return offsetOut[nodeId+1] - offsetOut[nodeId];
    }

    public int getX(int nodeId) {return  xs[nodeId];}

    public int getY(int nodeId) {return  ys[nodeId];}

    protected void setNodeCoords(int nodeId, int x, int y) {
        xs[nodeId] = x;
        ys[nodeId] = y;
    }

    public int getOutEdgeId(int nodeId, int edgeNum) {
        return offsetOut[nodeId] + edgeNum;
    }

    public int getEdgeCount() {
        return edgeCount;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public DrawData getDraw() {
        return draw;
    }

    public static CoreGraph readJson(ObjectMapper mapper, InputStream in, RequestParams requestParams) throws IOException{
        final JsonParser jp = mapper.getFactory().createParser(in);
        jp.setCodec(mapper);

        if (jp.nextToken() != JsonToken.START_OBJECT) {
            throw new JsonParseException("Result contains no json object", jp.getCurrentLocation());
        }

        String fieldname;
        JsonToken token;
        boolean finished = false;
        CoreGraph result = null;
        DrawData draw = null;
        int nodeCount = -1;
        int edgeCount = -1;
        int numEdges = 0;
        while (!finished) {
            //move to next field or END_OBJECT/EOF
            token = jp.nextToken();
            if (token == JsonToken.FIELD_NAME) {
                fieldname = jp.getCurrentName();
                token = jp.nextToken(); // move to value, or
                // START_OBJECT/START_ARRAY
                if ("nodeCount".equals(fieldname)) {
                    nodeCount = jp.getIntValue();
                } else if ("edgeCount".equals(fieldname)) {
                    edgeCount = jp.getIntValue();
                } else if ("draw".equals(fieldname)) {
                    draw = DrawData.readJson(jp, token);
                } else if ("edges".equals(fieldname)) {
                    if (edgeCount < 0 || nodeCount < 0 || draw == null) {
                        throw new JsonParseException("nodeCountHint, edgeCount and draw need to come before the edges themselves", jp.getCurrentLocation());
                    }
                    result = new CoreGraph(requestParams, nodeCount, edgeCount, draw);
                    // Should be on START_ARRAY
                    if (token != JsonToken.START_ARRAY) {
                        throw new JsonParseException("edges is no array", jp.getCurrentLocation());
                    }

                    while ((token = jp.nextToken()) != JsonToken.END_ARRAY) {
                        IntArrayList path = new IntArrayList();
                        int src = jp.getIntValue();
                        int trgt = jp.nextIntValue(0);
                        int cost = jp.nextIntValue(0);

                        // Should be on START_ARRAY
                        if (jp.nextToken() != JsonToken.START_ARRAY) {
                            throw new JsonParseException("draw is no array", jp.getCurrentLocation());
                        }

                        while (jp.nextToken() != JsonToken.END_ARRAY) {
                            // TODO Error checking i.e. for too few parameters would be kinda nice
                            path.add(jp.getIntValue());
                        }
                        result.setEdge(numEdges, src, trgt, cost, path);
                        if(path.size() > 0){
                            int firstDrawEdgeId = path.get(0);
                            result.setNodeCoords(src, draw.getX1(firstDrawEdgeId), draw.getY1(firstDrawEdgeId));
                            int lastDrawEdgeId = path.get(path.size()-1);
                            result.setNodeCoords(trgt, draw.getX2(lastDrawEdgeId), draw.getY2(lastDrawEdgeId));
                        }
                        numEdges++;
                    }
                }
            } else if (token == JsonToken.END_OBJECT) {
                // Normal end of request
                finished = true;
            } else if (token == null) {
                //EOF
                throw new JsonParseException("Unexpected EOF in Request", jp.getCurrentLocation());
            } else {
                throw new JsonParseException("Unexpected token " + token, jp.getCurrentLocation());
            }
        }
        if(result != null) {
            result.generateOffsets();
        }
        return result;
    }
}
