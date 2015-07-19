package chrenderclient.clientgraph;

import com.carrotsearch.hppc.IntArrayList;
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
    public IntArrayList path;

    public Edge(int src, int trgt, int cost, IntArrayList path) {
        this.src = src;
        this.trgt = trgt;
        this.cost = cost;
        this.path = path;
    }

    public static Edge readEdge(JsonParser jp, JsonToken token) throws IOException {
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


        if (src < 0 || trgt < 0) {
            throw new JsonParseException("Edge not complete", jp.getCurrentLocation());
        }
        return new Edge(src, trgt, cost, path);
    }
}
