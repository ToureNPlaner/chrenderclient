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
    private DrawData draw;

    // Debug data
    public long requestSize;


    private Bundle(int nodeCount, int upEdgeCount, int downEdgeCount, int coreSize, int level) {
        nodes = new Node[nodeCount];
        upEdges = new Edge[upEdgeCount];
        downEdges = new Edge[downEdgeCount];
        this.coreSize = coreSize;
        this.level = level;
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
            } else {
                throw new JsonParseException("Unexpected token " + token, jp.getCurrentLocation());
            }
        }

        if (nodeCount < 0 || upEdgeCount < 0) {
            throw new JsonParseException("Head not complete", jp.getCurrentLocation());
        }

        return new Bundle(nodeCount, upEdgeCount, downEdgeCount, coreSize, level);
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
                } else if ("edges".equals(fieldName)) {
                    if (bundle == null || bundle.draw == null) {
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
                    // and not in the core
                    if (e.src != currSrc) {
                        currSrc = e.src;
                        setOrCreateSourceNode(bundle, edgeNum, currSrc, e);
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
                        setOrCreateTargetNode(bundle, edgeNum, currTrgt, e);
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

    private static void setOrCreateSourceNode(Bundle bundle, int edgeNum, int currSrc, Edge e) {
        int index = currSrc - bundle.coreSize;
        if (bundle.nodes[index] == null) {
            Node node;
            if (e.path.size() > 0) {
                int firstDrawIndex = e.path.get(0);
                node = new Node(bundle.draw.getX1(firstDrawIndex), bundle.draw.getY1(firstDrawIndex), currSrc);
            } else { // Node not in the visible area thus has no coordinates
                node = new Node(currSrc);
            }
            bundle.nodes[index] = node;
        }
        bundle.nodes[index].setUpIndex(edgeNum);
    }

    private static void setOrCreateTargetNode(Bundle bundle, int edgeNum, int currTrgt, Edge e) {
        int index = currTrgt - bundle.coreSize;
        if (bundle.nodes[index] == null) {
            Node node;
            if (e.path.size() > 0) {
                int lastDrawIndex = e.path.get(e.path.size() - 1);
                node = new Node(bundle.draw.getX2(lastDrawIndex), bundle.draw.getY2(lastDrawIndex), currTrgt);
            } else { // Node not in the visible area thus has no coordinates
                node = new Node(currTrgt);
            }

            bundle.nodes[index] = node;
        }
        bundle.nodes[index].setDownIndex(edgeNum);
    }

    public BoundingBox getBbox() {
        return draw.getBbox();
    }
}
