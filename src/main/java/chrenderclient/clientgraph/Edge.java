package chrenderclient.clientgraph;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

/**
 * Created by niklas on 31.03.15.
 */
public final class Edge {
    public int src;
    public int trgt;
    public int cost;
    public RefinedPath path;

    public Edge(int src, int trgt, int cost, RefinedPath path) {
        this.src = src;
        this.trgt = trgt;
        this.cost = cost;
        this.path = path;
    }

    public static Edge readEdge(JsonParser jp, JsonToken token) throws JsonParseException, IOException {
        String fieldName;
        int src = -1;
        int trgt = -1;
        int cost = -1;
        RefinedPath path = new RefinedPath();
        if (token != JsonToken.START_OBJECT) {
            throw new JsonParseException("edge is no object", jp.getCurrentLocation());
        }
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            fieldName = jp.getCurrentName();
            token = jp.nextToken();
            if ("src".equals(fieldName)) {
                src = jp.getIntValue();
            } else if ("trgt".equals(fieldName)) {
                trgt = jp.getIntValue();
            } else if ("cost".equals(fieldName)) {
                cost = jp.getIntValue();
            } else if ("draw".equals(fieldName)) {
                // Should be on START_ARRAY
                if (token != JsonToken.START_ARRAY) {
                    throw new JsonParseException("draw is no array", jp.getCurrentLocation());
                }

                while (jp.nextToken() != JsonToken.END_ARRAY) {
                    // TODO Error checking i.e. for too few parameters would be kinda nice
                    int srcX = jp.getIntValue();
                    int srcY = jp.nextIntValue(0);
                    int trgtX = jp.nextIntValue(0);
                    int trgtY = jp.nextIntValue(0);
                    // No nextFloatValue ?
                    jp.nextToken();
                    float type = jp.getFloatValue();
                    path.add(srcX, srcY, trgtX, trgtY, type);
                }
            } else {
                throw new JsonParseException("Unexpected token " + token, jp.getCurrentLocation());
            }

        }
        if(src < 0 || trgt < 0) {
            throw new JsonParseException("Edge not complete", jp.getCurrentLocation());
        }
        return new Edge(src, trgt, cost, path);
    }
}
