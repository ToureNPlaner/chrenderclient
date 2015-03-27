package chrenderclient;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;

import java.awt.geom.Rectangle2D;
import java.io.IOException;

/**
 * A ToureNPlaner Client as accessible Java API
 */
public class TPClient {
    private CloseableHttpClient httpClient;
    private String uri;

    public TPClient(String uri) {
        httpClient = HttpClients.createDefault();
        this.uri = uri;
    }


    public PrioResult BBPrioRequest(Rectangle2D.Double range, int minPrio) throws IOException {
        PrioResult res = null;
        HttpPost httpPost = new HttpPost(this.uri + "/algbbpriolimited");
        String bS = "{\"bbox\":" +
                "{\"x\":" + range.getX() + ",\"y\":" + range.getY() +
                ",\"width\":" + range.getWidth() + ",\"height\":" + range.getHeight() + "}," +
                "\"nodeCount\":1000,\"mode\":\"auto\",\"level\":0,\"minLen\":40,\"maxLen\":8000,\"maxRatio\":0.01}";
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
            res = PrioResult.readResultData(new ObjectMapper(), resEntity.getContent());
            // do something useful with the response body
            // and ensure it is fully consumed
            EntityUtils.consume(resEntity);
        } finally {
            response1.close();
        }
        return res;
    }
}
