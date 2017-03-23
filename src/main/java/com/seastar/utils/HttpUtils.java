package com.seastar.utils;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by osx on 16/12/14.
 */
public class HttpUtils {
    public static class HttpResp {
        public int code = 400;
        public String body = "";
    }

    private CloseableHttpClient httpClient;

    public HttpUtils() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, trustAllCerts, null);
            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", new SSLConnectionSocketFactory(ctx)).build();

            // create socket configuration
            SocketConfig socketConfig = SocketConfig.custom().setTcpNoDelay(true).build();

            // create request config
            RequestConfig requestConfig = RequestConfig.custom()
                    // 从管理器中获取连接的超时时间
                    .setConnectionRequestTimeout(5000)
                    // 建立连接的超时时间
                    .setConnectTimeout(5000)
                    // 连接建立后数据读取的超时时间
                    .setSocketTimeout(5000)
                    .build();

            // Quoting the HttpClient 4.3.3. reference: “If the Keep-Alive header is not present in the response, HttpClient assumes the connection can be kept alive indefinitely.”
            // To get around this, and be able to manage dead connections we need a customized strategy implementation and build it into the HttpClient.
            ConnectionKeepAliveStrategy ckaStrategy = new ConnectionKeepAliveStrategy() {
                @Override
                public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
                    HeaderElementIterator it = new BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE));
                    while (it.hasNext()) {
                        HeaderElement he = it.nextElement();
                        String param = he.getName();
                        String value = he.getValue();
                        if (value != null && param.equalsIgnoreCase("timeout")) {
                            try {
                                return Long.parseLong(value) * 1000;
                            } catch (NumberFormatException ignore) {
                                ignore.printStackTrace();
                            }
                        }
                    }
                    return 30 * 1000;
                }
            };

            HttpRequestRetryHandler retryHandler = new HttpRequestRetryHandler() {
                @Override
                public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                    // 重试超过2次，放弃
                    if (executionCount >= 2) {
                        return false;
                    }

                    return true;
                }
            };

            PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
            // The maximum number of connections allowed across all routes
            cm.setMaxTotal(100);
            // The maximum number of connections allowed for a route that has not been specified otherwise by a call to setMaxPerRoute.
            // Use setMaxPerRoute when you know the route ahead of time and setDefaultMaxPerRoute when you do not.
            cm.setDefaultMaxPerRoute(20);
            cm.setDefaultSocketConfig(socketConfig);

            httpClient = HttpClients.custom()
                    .setConnectionManager(cm)
                    .setDefaultRequestConfig(requestConfig)
                    .setRetryHandler(retryHandler)
                    .setKeepAliveStrategy(ckaStrategy)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public HttpResp get(String url, Map<String, String> headers) {
        HttpResp resp = new HttpResp();

        CloseableHttpResponse response = null;

        try {
            HttpGet httpGet = new HttpGet(url);
            // add header
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    httpGet.setHeader(entry.getKey(), entry.getValue());
                }
            }

            response = httpClient.execute(httpGet, HttpClientContext.create());
            resp.code = response.getStatusLine().getStatusCode();
            resp.body = EntityUtils.toString(response.getEntity());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return resp;
    }

    public HttpResp post(String url, Map<String, String> body, Map<String, String> headers) {
        // body
        List<NameValuePair> params = new ArrayList<>();
        for (Map.Entry<String, String> entry : body.entrySet()) {
            params.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }

        try {
            return post(url, new UrlEncodedFormEntity(params, "UTF-8"), headers);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public HttpResp post(String url, String body, Map<String, String> headers) {
        return post(url, new StringEntity(body, "UTF-8"), headers);
    }

    private HttpResp post(String url, AbstractHttpEntity entity, Map<String, String> headers) {
        HttpResp resp = new HttpResp();

        CloseableHttpResponse response = null;

        try {
            HttpPost httpPost = new HttpPost(url);
            // add header
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    httpPost.setHeader(entry.getKey(), entry.getValue());
                }
            }

            httpPost.setEntity(entity);
            response = httpClient.execute(httpPost, HttpClientContext.create());
            resp.code = response.getStatusLine().getStatusCode();
            resp.body = EntityUtils.toString(response.getEntity());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return resp;
    }
}
