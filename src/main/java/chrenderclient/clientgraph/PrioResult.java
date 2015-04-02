package chrenderclient.clientgraph;


import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class PrioResult {

    public final ArrayList<Edge> edges;

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
                if ("upEdges".equals(fieldname) || "downEdges".equals(fieldname)) {
                    // Should be on START_ARRAY
                    if (token != JsonToken.START_ARRAY) {
                        throw new JsonParseException("edges is no array", jp.getCurrentLocation());
                    }

                    while ((token = jp.nextToken()) != JsonToken.END_ARRAY) {
                        if (token != JsonToken.START_OBJECT) {
                            throw new JsonParseException("edge is no object", jp.getCurrentLocation());
                        }
                        Edge edge = new Edge();
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
                                    edge.path.add(jp.getIntValue(), jp.nextIntValue(0), jp.nextIntValue(0), jp.nextIntValue(0), jp.nextIntValue(0));
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