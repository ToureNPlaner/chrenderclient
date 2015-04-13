package chrenderclient;

import chrenderclient.clientgraph.CoreGraph;
import chrenderclient.clientgraph.Bundle;
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

import java.awt.geom.Rectangle2D;
import java.io.BufferedInputStream;
import java.io.IOException;

/**
 * A ToureNPlaner Client as accessible Java API
 */
public class TPClient {
    private CloseableHttpClient httpClient;
    private String uri;
    private ObjectMapper mapper;

    private CoreGraph core;

    public TPClient(String uri) {
        httpClient = HttpClients.createDefault();
        this.uri = uri;
        this.core = null;;
        this.mapper = new ObjectMapper(new SmileFactory());
    }

    public CoreGraph getCore(int coreSize) throws IOException{
        // In the future we need to check that the core matches the graph
        // on the server if we store cores for longer
        if (core != null && core.getNodeCount() == coreSize) {
            return core;
        }

        core = coreRequest(coreSize);

        return core;
    }

    private CoreGraph coreRequest(int nodeCount) throws IOException{
        CoreGraph res = null;
        HttpPost httpPost = new HttpPost(this.uri + "/algdrawcore");
        httpPost.addHeader("Accept", "application/x-jackson-smile");
        String bS = "{\"nodeCount\":" +nodeCount+"}";
        System.err.println(bS);
        byte[] b = bS.getBytes("UTF-8");
        HttpEntity body = new ByteArrayEntity(b, ContentType.APPLICATION_JSON);
        httpPost.setEntity(body);
        CloseableHttpResponse response1 = httpClient.execute(httpPost);
        // The underlying HTTP connection is still held by the response object
        // to allow the response content to be streamed directly from the network socket.
        // In order to ensure correct deallocation of system resources
        // the user MUST call CloseableHttpResponse#close() from a finally clause.
        // Please note that if response content is not fully consumed the underlying
        // connection cannot be safely re-used and will be shut down and discarded
        // by the connection manager.
        try {
            System.out.println(response1.getStatusLine());
            HttpEntity resEntity = response1.getEntity();
            res = CoreGraph.readJSON(mapper, resEntity.getContent());
            // do something useful with the response body
            // and ensure it is fully consumed
            EntityUtils.consume(resEntity);
        } finally {
            response1.close();
        }
        return res;
    }

    public Bundle bbBundleRequest(Rectangle2D.Double range, int minPrio) throws IOException {
        Bundle res = null;
        HttpPost httpPost = new HttpPost(this.uri + "/algbbbundle");
        httpPost.addHeader("Accept", "application/x-jackson-smile");
        String bS = "{\"bbox\":" +
                "{\"x\":" + range.getX() + ",\"y\":" + range.getY() +
                ",\"width\":" + range.getWidth() + ",\"height\":" + range.getHeight() + "}," +
                "\"nodeCount\":1000,\"mode\":\"exact\",\"level\":"+minPrio+",\"minLen\":2,\"maxLen\":40,\"maxRatio\":0.01, " +
                "\"coreSize\" : "+((core != null)?core.getNodeCount():0)+"}";
        System.err.println(bS);
        byte[] b = bS.getBytes("UTF-8");
        HttpEntity body = new ByteArrayEntity(b, ContentType.APPLICATION_JSON);
        httpPost.setEntity(body);
        CloseableHttpResponse response1 = httpClient.execute(httpPost);
        // The underlying HTTP connection is still held by the response object
        // to allow the response content to be streamed directly from the network socket.
        // In order to ensure correct deallocation of system resources
        // the user MUST call CloseableHttpResponse#close() from a finally clause.
        // Please note that if response content is not fully consumed the underlying
        // connection cannot be safely re-used and will be shut down and discarded
        // by the connection manager.
        try {
            System.out.println(response1.getStatusLine());
            HttpEntity resEntity = response1.getEntity();
            long time0 = System.nanoTime();
            res = Bundle.readResultData(mapper, new BufferedInputStream(resEntity.getContent()));
            long time1 = System.nanoTime();
            System.out.println("Reading took "+(time1-time0)/1000000.0+" ms");
            // do something useful with the response body
            // and ensure it is fully consumed
            EntityUtils.consume(resEntity);
        } finally {
            response1.close();
        }
        return res;
    }
}
