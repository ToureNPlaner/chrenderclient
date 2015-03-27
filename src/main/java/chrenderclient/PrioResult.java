package chrenderclient;


import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class PrioResult {
    public static class DrawEdge {
        public final Point p1;
        public final Point p2;
        public final int type;

        public DrawEdge(int p1x, int p1y, int p2x, int p2y, int type) {
            p1 = new Point(p1x, p1y);
            p2 = new Point(p2x, p2y);
            this.type = type;
        }
    }

    public static class Edge {
        public int src;
        public int trgt;
        public int cost;
        public ArrayList<DrawEdge> draw;

        public Edge() {
            draw = new ArrayList<DrawEdge>();
        }
    }

    public final ArrayList<PrioResult.Edge> edges;

    public PrioResult() {
        edges = new ArrayList<Edge>();
    }

    public static PrioResult readResultData(ObjectMapper mapper, InputStream in) throws IOException {

        final JsonParser jp = mapper.getJsonFactory().createJsonParser(in);
        jp.setCodec(mapper);

        if (jp.nextToken() != JsonToken.START_OBJECT) {
            throw new JsonParseException("Result contains no json object", jp.getCurrentLocation());
        }

        String fieldname;
        JsonToken token;
        boolean finished = false;
        PrioResult result = new PrioResult();
        while (!finished) {
            //move to next field or END_OBJECT/EOF
            token = jp.nextToken();
            if (token == JsonToken.FIELD_NAME) {
                fieldname = jp.getCurrentName();
                token = jp.nextToken(); // move to value, or
                // START_OBJECT/START_ARRAY
                if ("edges".equals(fieldname)) {
                    // Should be on START_ARRAY
                    if (token != JsonToken.START_ARRAY) {
                        throw new JsonParseException("edges is no array", jp.getCurrentLocation());
                    }

                    while ((token = jp.nextToken()) != JsonToken.END_ARRAY) {
                        if (token != JsonToken.START_OBJECT) {
                            throw new JsonParseException("edge is no object", jp.getCurrentLocation());
                        }
                        PrioResult.Edge edge = new Edge();
                        while (jp.nextToken() != JsonToken.END_OBJECT) {
                            fieldname = jp.getCurrentName();
                            token = jp.nextToken();
                            if ("src".equals(fieldname)) {
                                edge.src = jp.getIntValue();
                            } else if ("trgt".equals(fieldname)) {
                                edge.trgt = jp.getIntValue();
                            } else if ("cost".equals(fieldname)) {
                                edge.cost = jp.getIntValue();
                            } else if ("draw".equals(fieldname)) {
                                // Should be on START_ARRAY
                                if (token != JsonToken.START_ARRAY) {
                                    throw new JsonParseException("draw is no array", jp.getCurrentLocation());
                                }

                                while (jp.nextToken() != JsonToken.END_ARRAY) {
                                    // TODO Error checking i.e. for too few parameters would be kinda nice
                                    edge.draw.add(new DrawEdge(jp.getIntValue(), jp.nextIntValue(0), jp.nextIntValue(0), jp.nextIntValue(0), jp.nextIntValue(0)));
                                }
                            }
                        }
                        result.edges.add(edge);
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
