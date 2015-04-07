package chrenderclient.clientgraph;


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
    private int nodeCount;
    private int edgeCount;

    private int[] offsetOut;

    private RefinedPath[] paths;
    private int[] srcs;
    private int[] trgts;
    private int[] costs;
    private int[] oEdgeIds;

    public CoreGraph(int nodeCount, int edgeCount) {
        this.nodeCount = nodeCount;
        this.edgeCount = edgeCount;

        srcs = new int[edgeCount];
        trgts = new int[edgeCount];
        costs = new int[edgeCount];
        oEdgeIds = new int[edgeCount];
        paths = new RefinedPath[edgeCount];
    }

    private void generateOutEdgeOffsets() {
        int currentSource;
        int prevSource = -1;
        this.offsetOut = new int[nodeCount + 1];
        for (int i = 0; i < edgeCount; i++) {
            currentSource = srcs[i];
            if (currentSource != prevSource) {
                for (int j = currentSource; j > prevSource; j--) {
                    offsetOut[j] = i;
                }
                prevSource = currentSource;
            }
        }

        offsetOut[nodeCount] = edgeCount;
        // assuming we have at least one edge
        for (int cnt = nodeCount - 1; offsetOut[cnt] == 0; cnt--) {
            offsetOut[cnt] = offsetOut[cnt + 1];
        }
    }

    private void setEdge(int pos, int src, int trgt, int cost, int oEdgeId, RefinedPath path) {
        srcs[pos] = src;
        trgts[pos] = trgt;
        costs[pos] = cost;
        oEdgeIds[pos] = oEdgeId;
        paths[pos] = path;
    }

    public int getOriginalEdgeId(int edgeId) {
        return oEdgeIds[edgeId];
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

    public RefinedPath getRefinedPath(int edgeId) {
        return paths[edgeId];
    }

    public int getOutEdgeCount(int nodeId) {
        return offsetOut[nodeId+1] - offsetOut[nodeId];
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

    public static CoreGraph readJSON(ObjectMapper mapper, InputStream in) throws IOException{
        final JsonParser jp = mapper.getFactory().createParser(in);
        jp.setCodec(mapper);

        if (jp.nextToken() != JsonToken.START_OBJECT) {
            throw new JsonParseException("Result contains no json object", jp.getCurrentLocation());
        }

        String fieldname;
        JsonToken token;
        boolean finished = false;
        CoreGraph result = null;
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
                } else if ("edges".equals(fieldname)) {
                    if (edgeCount < 0 || nodeCount < 0) {
                        throw new JsonParseException("nodeCount and edgeCount need to come before the edges themselves", jp.getCurrentLocation());
                    }
                    result = new CoreGraph(nodeCount, edgeCount);
                    // Should be on START_ARRAY
                    if (token != JsonToken.START_ARRAY) {
                        throw new JsonParseException("edges is no array", jp.getCurrentLocation());
                    }

                    while ((token = jp.nextToken()) != JsonToken.END_ARRAY) {
                        if (token != JsonToken.START_OBJECT) {
                            throw new JsonParseException("edge is no object", jp.getCurrentLocation());
                        }

                        RefinedPath path = new RefinedPath();
                        int src = 0;
                        int trgt = 0;
                        int cost = 0;
                        int oEdgeId = 0;

                        while (jp.nextToken() != JsonToken.END_OBJECT) {
                            fieldname = jp.getCurrentName();
                            token = jp.nextToken();
                            if ("path".equals(fieldname)) {
                                // Should be on START_ARRAY
                                if (token != JsonToken.START_ARRAY) {
                                    throw new JsonParseException("edges is no array", jp.getCurrentLocation());
                                }

                                while (jp.nextToken() != JsonToken.END_ARRAY) {
                                    // TODO Error checking i.e. for too few parameters would be kinda nice
                                    path.add(jp.getIntValue(), jp.nextIntValue(0), jp.nextIntValue(0), jp.nextIntValue(0), jp.nextIntValue(0));
                                }
                            } else if ("src".equals(fieldname)) {
                                src = jp.getIntValue();
                            } else if ("trgt".equals(fieldname)) {
                                trgt = jp.getIntValue();
                            } else if ("cost".equals(fieldname)) {
                                cost = jp.getIntValue();
                            } else if ("edgeId".equals(fieldname)) {
                                oEdgeId = jp.getIntValue();
                            }
                        }

                        result.setEdge(numEdges, src, trgt, cost, oEdgeId, path);
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
        return result;
    }
}
