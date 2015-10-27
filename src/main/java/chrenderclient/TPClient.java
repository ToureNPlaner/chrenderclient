package chrenderclient;

import chrenderclient.clientgraph.BoundingBox;
import chrenderclient.clientgraph.Bundle;
import chrenderclient.clientgraph.CoreGraph;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.BufferedInputStream;
import java.io.IOException;

/**
 * A ToureNPlaner Client as accessible Java API
 */
public class TPClient {
    private CloseableHttpClient httpClient;
    private String uri;
    private ObjectMapper mapper;

    public TPClient(String uri) {
        httpClient = HttpClients.createDefault();
        this.uri = uri;
        this.mapper = new ObjectMapper(new SmileFactory());
    }



    public CoreGraph coreRequest(int nodeCount, int minLen, int maxLen, double maxRatio) throws IOException {
        final CoreGraph.RequestParams requestParams = new CoreGraph.RequestParams(nodeCount, minLen, maxLen, maxRatio);

        HttpPost httpPost = new HttpPost(this.uri + "/algdrawcore");
        httpPost.addHeader("Accept", "application/x-jackson-smile");

        String bS = "{\"nodeCount\":" +requestParams.nodeCount +
                    ",\"minLen\":" + requestParams.minLen +
                    ",\"maxLen\":" + requestParams.maxLen +
                    ",\"maxRatio\":" + requestParams.maxRatio + "}";
        System.err.println(bS);
        byte[] b = bS.getBytes("UTF-8");
        HttpEntity body = new ByteArrayEntity(b, ContentType.APPLICATION_JSON);
        httpPost.setEntity(body);
        CloseableHttpResponse response = httpClient.execute(httpPost);
        // The underlying HTTP connection is still held by the response object
        // to allow the response content to be streamed directly from the network socket.
        // In order to ensure correct deallocation of system resources
        // the user MUST call CloseableHttpResponse#close() from a finally clause.
        // Please note that if response content is not fully consumed the underlying
        // connection cannot be safely re-used and will be shut down and discarded
        // by the connection manager.
        CoreGraph res = null;
        try {
            System.out.println(response.getStatusLine());
            HttpEntity resEntity = response.getEntity();
            long size = resEntity.getContentLength();
            long start = System.nanoTime();
            res = CoreGraph.readJson(mapper, resEntity.getContent(), requestParams);
            long end = System.nanoTime();
            res.requestSize = size;
            // do something useful with the response body
            // and ensure it is fully consumed
            double mibs = ((double) res.requestSize)/((double) (2<<20));
            System.out.println("COREDATA[Request Size (MiB),Time to Read (ms), Nodes, Edges, Draw Vertices, Draw Lines, Edge Paths Length]:"
                    +mibs+
                    ", "+Double.toString((end-start)/1000000.0)+
                    ", " +res.getNodeCount()+
                    ", "+res.getEdgeCount()+
                    ", "+res.getDraw().numVertices()+
                    ", "+res.getDraw().size()+
                    ", "+res.edgePathsLength);
            EntityUtils.consume(resEntity);
        } finally {
            response.close();
        }
        return res;
    }

    public Bundle bbBundleRequest(BoundingBox bbox, int coreSize, int minPrio, int minLen, int maxLen, double maxRatio, Bundle.LevelMode levelMode) throws IOException {
        Bundle.RequestParams requestParams = new Bundle.RequestParams(bbox, 600, coreSize, minPrio, minLen, maxLen, maxRatio, levelMode);

        String mode = levelMode.toString().toLowerCase();
        HttpPost httpPost = new HttpPost(this.uri + "/algbbbundle");
        httpPost.addHeader("Accept", "application/x-jackson-smile");
        String bS = "{\"bbox\":" +
                "{\"x\":" + requestParams.bbox.x + ",\"y\":" + requestParams.bbox.y +
                ",\"width\":" + requestParams.bbox.width + ",\"height\":" + requestParams.bbox.height + "}," +

                "\"nodeCountHint\":" + requestParams.nodeCountHint +
                ",\"mode\":\"" + mode +
                "\",\"minPrio\":" + requestParams.minPrio +
                ",\"minLen\":" + requestParams.minLen +
                ",\"maxLen\":" + requestParams.maxLen +
                ",\"maxRatio\":" + requestParams.maxRatio +
                ",\"coreSize\" : " + coreSize + "}";
        System.err.println(bS);
        byte[] b = bS.getBytes("UTF-8");
        HttpEntity body = new ByteArrayEntity(b, ContentType.APPLICATION_JSON);
        httpPost.setEntity(body);
        CloseableHttpResponse response = httpClient.execute(httpPost);
        // The underlying HTTP connection is still held by the response object
        // to allow the response content to be streamed directly from the network socket.
        // In order to ensure correct deallocation of system resources
        // the user MUST call CloseableHttpResponse#close() from a finally clause.
        // Please note that if response content is not fully consumed the underlying
        // connection cannot be safely re-used and will be shut down and discarded
        // by the connection manager.
        Bundle res = null;
        try {
            System.out.println(response.getStatusLine());
            HttpEntity resEntity = response.getEntity();
            long size = resEntity.getContentLength();
            long start = System.nanoTime();
            res = Bundle.readJson(mapper, new BufferedInputStream(resEntity.getContent()), requestParams);
            long end = System.nanoTime();
            res.readTimeNano = end-start;
            res.requestSize = size;
            double mibs = ((double) res.requestSize)/((double) (2<<20));
            System.out.println("BUNDLEDATA[Request Size (MiB),Time to Read (ms), Core Size, Level, Nodes, Edges, Upwards Edges, Downwards Edges, Draw Vertices, Draw Lines, Edge Paths Length]:"
                    +mibs+
                    ", "+Double.toString(res.readTimeNano/1000000.0)+
                    ", "+res.getCoreSize()+
                    ", "+res.level+
                    ", " +res.nodes.length+
                    ", "+(res.upEdges.length+res.downEdges.length)+
                    ", "+res.upEdges.length+
                    ", "+res.downEdges.length+
                    ", "+res.getDraw().numVertices()+
                    ", "+res.getDraw().size()+
                    ", "+res.edgePathsLength);
            // do something useful with the response body
            // and ensure it is fully consumed
            EntityUtils.consume(resEntity);
        } finally {
            response.close();
        }
        return res;
    }
}
