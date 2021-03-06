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


    public enum LevelMode {
        AUTO, HINTED, EXACT
    }

    public static final class RequestParams {
        public RequestParams(BoundingBox bbox, int nodeCountHint, int coreSize, int minPrio, int minLen, int maxLen, double maxRatio, LevelMode mode) {
            this.bbox = bbox;
            this.nodeCountHint = nodeCountHint;
            this.coreSize = coreSize;
            this.minPrio = minPrio;
            this.minLen = minLen;
            this.maxLen = maxLen;
            this.maxRatio = maxRatio;
            this.mode = mode;
        }
        public final BoundingBox bbox;
        public final int coreSize;
        public final int minPrio;
        public final int nodeCountHint;
        public final int minLen;
        public final int maxLen;
        public final double maxRatio;
        public final LevelMode mode;
    }

    public final RequestParams requestParams;
    public final Edge[] upEdges;
    public final Edge[] downEdges;
    public final Node[] nodes;
    public final int level;
    private final int coreSize;
    private DrawData draw;

    // Debug data
    public long requestSize;
    public long readTimeNano;
    public long edgePathsLength = 0;


    private Bundle(RequestParams requestParams, int level, int nodeCount, int upEdgeCount, int downEdgeCount) {
        this.requestParams = requestParams;
        nodes = new Node[nodeCount];
        upEdges = new Edge[upEdgeCount];
        downEdges = new Edge[downEdgeCount];
        this.level = level;
        this.coreSize = requestParams.coreSize;
        this.draw = null;
    }

    public int getCoreSize(){
        return coreSize;
    }

    public DrawData getDraw() {
        return draw;
    }

    private static BoundingBox readBoundingBox(JsonParser jp, JsonToken token) throws IOException {
        String fieldName;
        int x = 0;
        int y = 0;
        int width = 0;
        int height = 0;
        if (token != JsonToken.START_OBJECT) {
            throw new JsonParseException("head is no object", jp.getCurrentLocation());
        }
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            fieldName = jp.getCurrentName();
            token = jp.nextToken();
            if ("x".equals(fieldName)) {
                x = jp.getIntValue();
            } else if ("y".equals(fieldName)) {
                y = jp.getIntValue();
            } else if ("width".equals(fieldName)) {
                width = jp.getIntValue();
            } else if ("height".equals(fieldName)) {
                height = jp.getIntValue();
            } else {
                throw new JsonParseException("Unexpected token " + token, jp.getCurrentLocation());
            }
        }
        return new BoundingBox(x, y, width, height);
    }

    private static Bundle readPrioResultHead(JsonParser jp, JsonToken token, Bundle.RequestParams requestParams) throws IOException {
        String fieldName;
        int nodeCount = -1;
        int upEdgeCount = -1;
        int downEdgeCount = -1;
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
            } else if ("level".equals(fieldName)) {
                level = jp.getIntValue();
            } else {
                throw new JsonParseException("Unexpected token " + token, jp.getCurrentLocation());
            }
        }

        if (nodeCount < 0 || upEdgeCount < 0) {
            throw new JsonParseException("Head not complete", jp.getCurrentLocation());
        }

        return new Bundle(requestParams, level, nodeCount, upEdgeCount, downEdgeCount);
    }


    public static Bundle readJson(ObjectMapper mapper, InputStream in, Bundle.RequestParams requestParams) throws IOException {

        /*
        Prio Result looks something like this:
        [
        {"nodeCountHint" : NUM, "upEdgeCount": NUM, "downEdgeCount" : NUM},
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
                    bundle = readPrioResultHead(jp, token, requestParams);
                } else if ("draw".equals(fieldName)) {
                    if (bundle == null) {
                        throw new JsonParseException("Need to see head before draw", jp.getCurrentLocation());
                    }
                    bundle.draw = DrawData.readJson(jp, token);
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
                        bundle.nodes[currTrgt - bundle.getCoreSize()].setDownIndex(edgeNum);
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
        int index = currSrc - bundle.getCoreSize();
        assert bundle.nodes[index] != null;
        bundle.nodes[index].setCoords(bundle.draw.getX1(e.drawEdgeIndex), bundle.draw.getY1(e.drawEdgeIndex));
        bundle.nodes[index].setUpIndex(edgeNum);
    }
}
