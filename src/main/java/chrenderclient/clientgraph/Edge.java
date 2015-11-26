package chrenderclient.clientgraph;

import com.carrotsearch.hppc.IntArrayList;
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
    public int drawEdgeIndex;

    public Edge(int src, int trgt, int cost, int drawEdgeIndex) {
        this.src = src;
        this.trgt = trgt;
        this.cost = cost;
        this.drawEdgeIndex = drawEdgeIndex;
    }

    public static Edge readEdge(JsonParser jp, JsonToken token) throws IOException {
        IntArrayList path = new IntArrayList();
        int src = jp.getIntValue();
        int trgt = jp.nextIntValue(0);
        int cost = jp.nextIntValue(0);
        int drawEdgeIndex = jp.nextIntValue(0);
        return new Edge(src, trgt, cost, drawEdgeIndex);
    }
}
