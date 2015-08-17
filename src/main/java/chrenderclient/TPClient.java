package chrenderclient;

import chrenderclient.clientgraph.BoundingBox;
import chrenderclient.clientgraph.Bundle;
import chrenderclient.clientgraph.CoreGraph;
import chrenderclient.clientgraph.Utils;
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

    public CoreGraph coreRequest(int nodeCount, int minLen, int maxLen, double maxRatio) throws IOException{
        CoreGraph res = null;
        HttpPost httpPost = new HttpPost(this.uri + "/algdrawcore");
        httpPost.addHeader("Accept", "application/x-jackson-smile");
        String bS = "{\"nodeCount\":" +nodeCount+",\"minLen\":"+minLen+",\"maxLen\":"+maxLen+",\"maxRatio\":"+maxRatio+"}";
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
        try {
            System.out.println(response.getStatusLine());
            HttpEntity resEntity = response.getEntity();
            long size = resEntity.getContentLength();
            System.out.println("Core has requestSize:"+Utils.sizeForHumans(size));
            res = CoreGraph.readJson(mapper, resEntity.getContent());
            res.requestSize = size;
            // do something useful with the response body
            // and ensure it is fully consumed
            EntityUtils.consume(resEntity);
        } finally {
            response.close();
        }
        return res;
    }

    public Bundle bbBundleRequest(BoundingBox bbox, int coreSize, int minPrio, int minLen, int maxLen, double maxRatio, boolean AUTO) throws IOException {
        Bundle res = null;
        String mode = "exact";
        if(AUTO){
            mode = (minPrio == Integer.MAX_VALUE)? "auto":"hinted";
        }
        HttpPost httpPost = new HttpPost(this.uri + "/algbbbundle");
        httpPost.addHeader("Accept", "application/x-jackson-smile");
        final int nodeCount = 800;
        String bS = "{\"bbox\":" +
                "{\"x\":" + bbox.x + ",\"y\":" + bbox.y +
                ",\"width\":" + bbox.width + ",\"height\":" + bbox.height + "}," +
                "\"nodeCount\":"+nodeCount+",\"mode\":\""+mode+"\",\"level\":"+minPrio+",\"minLen\":"+minLen+",\"maxLen\":"+maxLen+",\"maxRatio\":"+maxRatio+", " +
                "\"coreSize\" : "+coreSize+"}";
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
        try {
            System.out.println(response.getStatusLine());
            HttpEntity resEntity = response.getEntity();
            long size = resEntity.getContentLength();
            long start = System.nanoTime();
            res = Bundle.readJson(mapper, new BufferedInputStream(resEntity.getContent()));
            res.requestSize = size;
            System.out.println(Utils.took("Reading Bundles", start));
            System.out.println("Bundle has requestSize:"+Utils.sizeForHumans(size));
            // do something useful with the response body
            // and ensure it is fully consumed
            EntityUtils.consume(resEntity);
        } finally {
            response.close();
        }
        return res;
    }
}
