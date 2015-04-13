package chrenderclient.clientgraph;


import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

public class Bundle {

    public final Edge[] upEdges;
    public final Edge[] downEdges;
    public final Node[] nodes;
    public final int coreSize;

    public Bundle(int nodeCount, int upEdgeCount, int downEdgeCount, int coreSize) {
        nodes = new Node[nodeCount];
        upEdges = new Edge[upEdgeCount];
        downEdges = new Edge[downEdgeCount];
        this.coreSize = coreSize;
    }


    private static Bundle readPrioResultHead(JsonParser jp, JsonToken token) throws IOException, JsonParseException {
        String fieldName;
        int nodeCount = -1;
        int upEdgeCount = -1;
        int downEdgeCount = -1;
        int coreSize = 0;
        RefinedPath path = new RefinedPath();
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
            } else {
                throw new JsonParseException("Unexpected token " + token, jp.getCurrentLocation());
            }
        }

        if (nodeCount < 0 || upEdgeCount < 0) {
            throw new JsonParseException("Head not complete", jp.getCurrentLocation());
        }

        return new Bundle(nodeCount, upEdgeCount, downEdgeCount, coreSize);
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
                } else if ("edges".equals(fieldName)) {
                    if(bundle == null) {
                        throw new JsonParseException("Need to see head before edges", jp.getCurrentLocation());
                    }
                    readEdges(jp, token, bundle);
                } else {
                    throw new JsonParseException("Unexpected field "+fieldName, jp.getCurrentLocation());
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
                        int index = currSrc - bundle.coreSize;
                        if(bundle.nodes[index] == null) {
                            bundle.nodes[index] = new Node(e.path.getX1(0), e.path.getY1(0));
                        }
                        bundle.nodes[index].upIndex = index;
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
                        int index = currTrgt - bundle.coreSize;
                        if(bundle.nodes[index] == null) {
                            bundle.nodes[index] = new Node(e.path.getX1(e.path.size() - 1), e.path.getY1(e.path.size() - 1));
                        }
                        bundle.nodes[index].downIndex = index;
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
}
