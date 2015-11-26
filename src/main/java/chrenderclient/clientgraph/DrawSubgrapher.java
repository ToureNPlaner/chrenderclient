package chrenderclient.clientgraph;

import java.util.Arrays;

/**
 * DrawSubgrapher allows to cut subgraphs from DrawData graphs into an existing DrawData graph
 * Created by niklas on 26.11.15.
 */
public final class DrawSubgrapher {
    private final DrawData source;
    private final DrawData target;
    private final int[] vertexMap;
    
    public DrawSubgrapher(DrawData source, DrawData target){
        this.source = source;
        this.target = target;
        this.vertexMap = new int[source.numVertices()];
        Arrays.fill(vertexMap, -1);
    }

    /**
     * Add a vertex from the source DrawData to the target DrawData if it wasn't mapped yet. Return its new/mapped id
     * @param vertexId
     * @return
     */
    private int addVertex(int vertexId){
        if(vertexMap[vertexId] >= 0){
            return vertexMap[vertexId];
        }
        return target.addVertex(source.getX(vertexId), source.getY(vertexId));
    }

    /**
     * Unpacks the subsumed edges of a skipping draw edge from the source DrawData to the target DrawData
     * and returns the edge index of the newly added edge
     */
    public final int unpackAndAdd(int drawEdgeIndex) {
        int srcId = source.getSource(drawEdgeIndex);
        srcId = addVertex(srcId);
        int trgtId = source.getTarget(drawEdgeIndex);
        trgtId = addVertex(trgtId);
        int type = source.getType(drawEdgeIndex);
        int drawScA = source.getDrawScA(drawEdgeIndex);
        int drawScB = source.getDrawScB(drawEdgeIndex);

        if (drawScA >= 0) {
            drawScA = unpackAndAdd(drawScA);
            drawScB = unpackAndAdd(drawScB);
        }

        return target.addEdge(srcId, trgtId, type, drawScA, drawScB);
    }
}
