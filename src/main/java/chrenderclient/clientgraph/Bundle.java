package chrenderclient.clientgraph;


import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

/**
 * A Bundle combines up- and downGraphs for all nodes withing a bounding box with level greater
 * some minimum level
 */
public class Bundle {

    public final Edge[] upEdges;
    public final Edge[] downEdges;
    public final Node[] nodes;
    public final int coreSize;
    public final int level;
    public final double minLen;
    public final double maxLen;
    public final double maxRatio;
    private DrawData draw;

    // Debug data
    public long requestSize;


    private Bundle(int nodeCount, int upEdgeCount, int downEdgeCount, int coreSize, int level, double minLen, double maxLen, double maxRatio) {
        nodes = new Node[nodeCount];
        upEdges = new Edge[upEdgeCount];
        downEdges = new Edge[downEdgeCount];
        this.coreSize = coreSize;
        this.level = level;
        this.minLen = minLen;
        this.maxLen = maxLen;
        this.maxRatio = maxRatio;
        this.draw = null;
    }

    public DrawData getDraw() {
        return draw;
    }

    private static Bundle readPrioResultHead(JsonParser jp, JsonToken token) throws IOException {
        String fieldName;
        int nodeCount = -1;
        int upEdgeCount = -1;
        int downEdgeCount = -1;
        int coreSize = 0;
        int level = 0;
        double minLen = 20;
        double maxLen = 400;
        double maxRatio = 0.01;
        if (token != JsonToken.START_OBJECT) {
            throw new JsonParseException("head is no object", jp.getCurrentLocation());
        }
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            fieldName = jp.getCurrentName();
            token = jp.nextToken();
            if ("nodeCount".equals(fieldName)) {
                nodeCount = jp.getIntValue();
            } else if ("upEdgeCount".equals(fieldName)) {
                upEdgeCount = jp.getIntValue();
            } else if ("downEdgeCount".equals(fieldName)) {
                downEdgeCount = jp.getIntValue();
            } else if ("coreSize".equals(fieldName)) {
                coreSize = jp.getIntValue();
            } else if ("level".equals(fieldName)) {
                level = jp.getIntValue();
            } else if ("minLen".equals(fieldName)) {
                minLen = jp.getDoubleValue();
            } else if ("maxLen".equals(fieldName)) {
                maxLen = jp.getDoubleValue();
            } else if ("maxRatio".equals(fieldName)) {
                maxRatio = jp.getDoubleValue();
            } else {
                throw new JsonParseException("Unexpected token " + token, jp.getCurrentLocation());
            }
        }

        if (nodeCount < 0 || upEdgeCount < 0) {
            throw new JsonParseException("Head not complete", jp.getCurrentLocation());
        }

        return new Bundle(nodeCount, upEdgeCount, downEdgeCount, coreSize, level, minLen, maxLen, maxRatio);
    }


    public static Bundle readResultData(ObjectMapper mapper, InputStream in) throws IOException {

        /*
        Prio Result looks something like this:
        [
        {"nodeCount" : NUM, "upEdgeCount": NUM, "downEdgeCount" : NUM},
        {
        "upEdges" : ...,,
        "downEdges" : ....
        }
        ]
         */

        final JsonParser jp = mapper.getFactory().createParser(in);
        jp.setCodec(mapper);

        if (jp.nextToken() != JsonToken.START_OBJECT) {
            throw new JsonParseException("PrioResult contains no json object", jp.getCurrentLocation());
        }

        String fieldName;
        JsonToken token;
        Bundle bundle = null;
        boolean nodeIdsRead = false;
        while (true) {
            //move to next element or END_OBJECT/EOF
            token = jp.nextToken();
            if (token == JsonToken.FIELD_NAME) {
                fieldName = jp.getCurrentName();
                token = jp.nextToken(); // move to value, or
                // START_OBJECT/START_ARRAY

                if ("head".equals(fieldName)) {
                    bundle = readPrioResultHead(jp, token);
                } else if ("draw".equals(fieldName)) {
                    if (bundle == null) {
                        throw new JsonParseException("Need to see head before draw", jp.getCurrentLocation());
                    }
                    bundle.draw = DrawData.readDrawData(jp, token);
                } else if("oNodeIds".equals(fieldName)){
                    if (bundle == null || bundle.draw == null) {
                        throw new JsonParseException("Need to see head and draw before original node ids", jp.getCurrentLocation());
                    }
                    readOriginalNodeIds(jp, token, bundle);
                    nodeIdsRead = true;
                } else if ("edges".equals(fieldName)) {
                    if (bundle == null || bundle.draw == null || nodeIdsRead == false) {
                        throw new JsonParseException("Need to see head and draw before edges", jp.getCurrentLocation());
                    }
                    readEdges(jp, token, bundle);
                } else {
                    throw new JsonParseException("Unexpected field " + fieldName, jp.getCurrentLocation());
                }
            } else if (token == JsonToken.END_OBJECT) {
                // Normal end of request
                break;
            } else if (token == null) {
                //EOF
                throw new JsonParseException("Unexpected EOF in Request", jp.getCurrentLocation());
            } else {
                throw new JsonParseException("Unexpected token " + token, jp.getCurrentLocation());
            }
        }
        return bundle;

    }

    private static void readOriginalNodeIds(JsonParser jp, JsonToken token, Bundle bundle) throws IOException {
        if (token != JsonToken.START_ARRAY) {
            throw new JsonParseException("oNodeIds is no Json array", jp.getCurrentLocation());
        }
        int i = 0;
        int oId;
        while (jp.nextToken() != JsonToken.END_ARRAY) {
            oId = jp.getIntValue();
            bundle.nodes[i] = new Node(oId);
            i++;
        }

        if(i < bundle.nodes.length){
            throw new JsonParseException("oNodeIds has less entries than nodes", jp.getCurrentLocation());
        }
    }

    private static void readEdges(JsonParser jp, JsonToken token, Bundle bundle) throws IOException {
        String fieldname;
        if (token != JsonToken.START_OBJECT) {
            throw new JsonParseException("edges is no Json object", jp.getCurrentLocation());
        }

        while (jp.nextToken() != JsonToken.END_OBJECT) {
            fieldname = jp.getCurrentName();
            token = jp.nextToken(); // move to value, or

            if ("upEdges".equals(fieldname)) {
                // Should be on START_ARRAY
                int edgeNum = 0;
                int currSrc = -1;
                if (token != JsonToken.START_ARRAY) {
                    throw new JsonParseException("edges is no array", jp.getCurrentLocation());
                }

                while ((token = jp.nextToken()) != JsonToken.END_ARRAY) {
                    Edge e = Edge.readEdge(jp, token);
                    // We need to extract the x, y coordinates of the source node, guaranteed to be in PrioResult
                    // and not in the core, every node is a source somewhere (no trapping streets)
                    if (e.src != currSrc) {
                        currSrc = e.src;
                        setSourceCoords(bundle, edgeNum, currSrc, e);
                    }
                    bundle.upEdges[edgeNum] = e;
                    edgeNum++;
                }
                if (edgeNum < bundle.upEdges.length) {
                    throw new JsonParseException("Missing edges " + edgeNum + " of " + bundle.upEdges.length + " read", jp.getCurrentLocation());
                }
            } else if ("downEdges".equals(fieldname)) {
                // Should be on START_ARRAY
                int edgeNum = 0;
                int currTrgt = -1;
                if (token != JsonToken.START_ARRAY) {
                    throw new JsonParseException("edges is no array", jp.getCurrentLocation());
                }

                while ((token = jp.nextToken()) != JsonToken.END_ARRAY) {
                    Edge e = Edge.readEdge(jp, token);
                    // We need to extract the x, y coordinates of the target node, guaranteed to be in PrioResult
                    // and not in the core
                    if (e.trgt != currTrgt) {
                        currTrgt = e.trgt;
                        bundle.nodes[currTrgt - bundle.coreSize].setDownIndex(edgeNum);
                    }
                    bundle.downEdges[edgeNum] = e;
                    edgeNum++;
                }
                if (edgeNum < bundle.downEdges.length) {
                    throw new JsonParseException("Missing edges " + edgeNum + " of " + bundle.downEdges.length + " read", jp.getCurrentLocation());
                }
            }
        }
    }

    private static void setSourceCoords(Bundle bundle, int edgeNum, int currSrc, Edge e) {
        int index = currSrc - bundle.coreSize;
        assert bundle.nodes[index] != null;
        if(e.path.size() > 0) {
            int firstDrawIndex = e.path.get(0);
            bundle.nodes[index].setCoords(bundle.draw.getX1(firstDrawIndex), bundle.draw.getY1(firstDrawIndex));
        }
        bundle.nodes[index].setUpIndex(edgeNum);
    }

    public BoundingBox getBbox() {
        return draw.getBbox();
    }
}
