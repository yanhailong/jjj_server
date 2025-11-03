package com.jjg.game.common.utils;

import cn.hutool.http.ContentType;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class HttpUtils {

    public static int CREATE_TIME_OUT = 15000, RESPONSE_TIME_OUT = 30000;

    static Logger log = LoggerFactory.getLogger(HttpUtils.class);

    // 针对多线程的情况下一个connection可以执行多个方法，但是这种情况每次执行完方法都必须将连接释放掉/
    // maxConnectionsPerHost：每个Host最大连接数，默认是2。
    // maxTotalConnections：最大活动连接数，默认是20。
    private static MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();

    private static HttpClient httpClient = new HttpClient(connectionManager);

    {
        // 初始化最大连接数
        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        params.setDefaultMaxConnectionsPerHost(100);
        params.setMaxTotalConnections(200);
        connectionManager.setParams(params);
    }

    /**
     * post 请求，使用默认的header信息与UTF-8编码格式，其他请使用
     * {@link #doPost(String, Map, Map, String)}
     *
     * @param url  请求url
     * @param para 请求参数
     * @return
     * @throws Exception
     */
    public static HttpResponse doPost(String url, Map<String, String> para)
            throws Exception {
        return doPost(url, para, null, null);
    }

    public static HttpResponse doPost(String url , String params , String contentType ,String charset) throws Exception {
        PostMethod postMethod = new PostMethod(url);
        RequestEntity requestEntity = new StringRequestEntity(params,contentType,charset);
        postMethod.setRequestEntity(requestEntity);
        HttpResponse httpResponse = new HttpResponse();
        Exception exception = null;
        try {
            httpClient.getHttpConnectionManager().getParams()
                    .setConnectionTimeout(CREATE_TIME_OUT);// 建立链接10秒超时
            httpClient.getHttpConnectionManager().getParams()
                    .setSoTimeout(RESPONSE_TIME_OUT); // 数据返回10秒超时
            int statusCode = httpClient.executeMethod(postMethod);
            byte[] responseBody = postMethod.getResponseBody();
            httpResponse.setStatusCode(statusCode);
            httpResponse.setResponseBody(responseBody);
        } catch (HttpException e) {
            exception = e;
            log.warn("executePostRequest httpException", e);
        } catch (IOException e) {
            exception = e; // 这里会捕捉到连接超时和数据返回超时异常ConnectTimeoutException，SocketTimeoutException
            log.warn("executePostRequest IOException", e);
        } catch (Exception e) {
            exception = e;
            log.warn("executePostRequest Exception", e);
        } finally {
            postMethod.releaseConnection();// 关闭连接
            if (exception != null)
                throw exception;
        }
        return httpResponse;
    }

    public static HttpResponse doPostWithXml(String url , String params) throws Exception{
        String defaultHttpContentCharset = "UTF-8";
        String defaultHttpContentType = "text/xml";
        return doPost(url,params,defaultHttpContentType,defaultHttpContentCharset);
    }

    public static HttpResponse doPostWithJSON(String url , String params) throws Exception{
        String defaultHttpContentCharset = "UTF-8";
        String defaultHttpContentType = ContentType.JSON.toString();
        return doPost(url,params,defaultHttpContentType,defaultHttpContentCharset);
    }

    /**
     * 发送POST请求
     *
     * @param url                  请求的url链接
     * @param para                 post需要带上的表单参数，里面采用键值对的方式key-value
     * @param headers              请求头
     * @param http_content_charset http传输内容编码字符集,如果传null。默认采用UTF-8
     * @return
     * @throws Exception
     */
    public static HttpResponse doPost(String url, Map<String, String> para,
                                      Map<String, String> headers, String http_content_charset)
            throws Exception {

        String defaultHttpContentCharset = "UTF-8";

        if (http_content_charset != null
                && !"".equals(http_content_charset.trim())) {
            defaultHttpContentCharset = http_content_charset;
        }

        PostMethod postMethod = new PostMethod(url);
        HttpResponse httpResponse = new HttpResponse();
        Exception exception = null;

        // 如果带有参数，则进行参数设置
        if (para != null && para.size() > 0) {

            NameValuePair[] valuePairArray = new NameValuePair[para.size()];
            Set<String> paraSet = para.keySet();
            Iterator<String> iterator = paraSet.iterator();
            int i = 0;
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                NameValuePair pair = new NameValuePair(key, para.get(key));
                valuePairArray[i] = pair;
                i++;
            }
            postMethod.setRequestBody(valuePairArray);
        }
        // 如果有设置header，则增加header
        if (headers != null && headers.size() > 0) {
            Set<String> headerSet = headers.keySet();
            Iterator<String> iterator = headerSet.iterator();
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                postMethod.addRequestHeader(key, headers.get(key));
            }
        }
        // 发送请求
        try {
            // 设置发送时候的最终编码是UTF-8(不然默认的是ISO-8859-1)
            httpClient.getParams().setParameter(
                    HttpMethodParams.HTTP_CONTENT_CHARSET,
                    defaultHttpContentCharset);
            httpClient.getHttpConnectionManager().getParams()
                    .setConnectionTimeout(CREATE_TIME_OUT);// 建立链接10秒超时
            httpClient.getHttpConnectionManager().getParams()
                    .setSoTimeout(RESPONSE_TIME_OUT); // 数据返回10秒超时
            int statusCode = httpClient.executeMethod(postMethod);
            byte[] responseBody = postMethod.getResponseBody();
            // System.out.println(new String(responseBody));
            httpResponse.setStatusCode(statusCode);
            httpResponse.setResponseBody(responseBody);

        } catch (HttpException e) {
            exception = e;
            log.warn("executePostRequest httpException", e);
        } catch (IOException e) {
            exception = e; // 这里会捕捉到连接超时和数据返回超时异常ConnectTimeoutException，SocketTimeoutException
            log.warn("executePostRequest IOException", e);
        } catch (Exception e) {
            exception = e;
            log.warn("executePostRequest Exception", e);
        } finally {
            postMethod.releaseConnection();// 关闭连接
            if (exception != null)
                throw exception;
        }

        return httpResponse;
    }

    public static byte[] getBody(PostMethod postMethod) throws IOException {
        InputStream inputStream = postMethod.getResponseBodyAsStream();
        byte[] bytes = new byte[1024];
        int n;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        while ((n = inputStream.read(bytes)) != -1) {
            bout.write(bytes, 0, n);
        }
        return bout.toByteArray();
    }

    /**
     * 使用http get请求
     * 使用默认的头信息与UTF8编码，如果需要自定义头信息与编码消息，请使用
     * {@link #doGet(String, Map, Map, String, String)}
     *
     * @param url  请求地址
     * @param para 请求参数
     * @return
     * @throws Exception
     */
    public static HttpResponse doGet(String url, Map<String, String> para)
            throws Exception {
        return doGet(url, para, null, null, null);
    }

    /**
     * 发送GET请求
     *
     * @param url                  请求location
     * @param para                 url跟在后面的参数 map,里面采用键值对的方式key-value
     * @param headers              请求头 map,里面采用键值对的方式key-value
     * @param url_encode_charset   url格式编码字符集 如果传null。默认采用UTF-8
     * @param http_content_charset http传输内容编码字符集 如果传null。默认采用UTF-8
     * @return
     * @throws Exception
     */
    public static HttpResponse doGet(String url, Map<String, String> para,
                                     Map<String, String> headers, String url_encode_charset,
                                     String http_content_charset) throws Exception {
        String urlParam = "";
        String defaultUrlEncodeCharset = "UTF-8";
        String defaultHttpContentCharset = "UTF-8";

        if (url_encode_charset != null
                && !"".equals(url_encode_charset.trim())) {
            defaultUrlEncodeCharset = url_encode_charset;
        }

        if (http_content_charset != null
                && !"".equals(http_content_charset.trim())) {
            defaultHttpContentCharset = http_content_charset;
        }

        HttpResponse httpResponse = new HttpResponse();
        Exception exception = null;

        // 如果带有参数，则进行参数设置
        if (para != null && para.size() > 0) {
            Set<String> paraSet = para.keySet();
            Iterator<String> iterator = paraSet.iterator();
            int i = 0;
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                String value = (String) para.get(key);
                if (i == 0) {
                    urlParam = urlParam + "?" + key + "="
                            + URLEncoder.encode(value, defaultUrlEncodeCharset);
                } else {
                    urlParam = urlParam + "&" + key + "="
                            + URLEncoder.encode(value, defaultUrlEncodeCharset);
                }
                i++;
            }
        }

        // 发送请求并取得数据
        GetMethod getMethod = new GetMethod(url + urlParam);

        // 如果有设置header，则增加header
        if (headers != null && headers.size() > 0) {
            Set<String> headerSet = headers.keySet();
            Iterator<String> iterator = headerSet.iterator();
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                getMethod.addRequestHeader(key, headers.get(key));
            }
        }

        try {
            // 设置成了默认的恢复策略，在发生异常时候将自动重试3次，在这里你也可以设置成自定义的恢复策略
            getMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                    new DefaultHttpMethodRetryHandler());
            // 设置发送时候的最终编码是UTF-8(不然默认的是ISO-8859-1)
            httpClient.getParams().setParameter(
                    HttpMethodParams.HTTP_CONTENT_CHARSET,
                    defaultHttpContentCharset);
            httpClient.getHttpConnectionManager().getParams()
                    .setConnectionTimeout(CREATE_TIME_OUT);// 建立链接10秒超时
            httpClient.getHttpConnectionManager().getParams()
                    .setSoTimeout(RESPONSE_TIME_OUT); // 数据返回10秒超时
            // 执行getMethod
            int statusCode = httpClient.executeMethod(getMethod);
            byte[] responseBody = getMethod.getResponseBody();

            httpResponse.setStatusCode(statusCode);
            httpResponse.setResponseBody(responseBody);

        } catch (Exception e) {
            log.warn("executeGetRequest exception", e);
            exception = e;
        } finally {
            getMethod.releaseConnection();// 关闭连接
            if (exception != null)
                throw exception;
        }
        return httpResponse;
    }

    /**
     * 发送POST请求
     *
     * @param url                  请求的url链接
     * @param postBodyContent      body内容使用json格式提交
     * @param headers              请求头
     * @param http_content_charset http传输内容编码字符集,如果传null。默认采用UTF-8
     * @return
     * @throws Exception
     */
    public static HttpResponse doPost(String url, String postBodyContent,
                                      Map<String, String> headers, String http_content_charset)
            throws Exception {

        String defaultHttpContentCharset = "UTF-8";

        if (http_content_charset != null
                && !"".equals(http_content_charset.trim())) {
            defaultHttpContentCharset = http_content_charset;
        }

        PostMethod postMethod = new PostMethod(url);
        HttpResponse httpResponse = new HttpResponse();
        Exception exception = null;

        // 设置body
        if (postBodyContent != null) {
            postMethod.setRequestEntity(new ByteArrayRequestEntity(
                    postBodyContent.getBytes(defaultHttpContentCharset)));
        }
        postMethod.addRequestHeader("Content Type", "application/json");
        // 如果有设置header，则增加header
        if (headers != null && headers.size() > 0) {
            Set<String> headerSet = headers.keySet();
            Iterator<String> iterator = headerSet.iterator();
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                postMethod.addRequestHeader(key, headers.get(key));
            }
        }
        // 发送请求
        try {
            // 设置发送时候的最终编码是UTF-8(不然默认的是ISO-8859-1)
            httpClient.getParams().setParameter(
                    HttpMethodParams.HTTP_CONTENT_CHARSET,
                    defaultHttpContentCharset);
            httpClient.getHttpConnectionManager().getParams()
                    .setConnectionTimeout(CREATE_TIME_OUT);// 建立链接10秒超时
            httpClient.getHttpConnectionManager().getParams()
                    .setSoTimeout(RESPONSE_TIME_OUT); // 数据返回10秒超时
            int statusCode = httpClient.executeMethod(postMethod);
            byte[] responseBody = postMethod.getResponseBody();
            // System.out.println(new String(responseBody));
            httpResponse.setStatusCode(statusCode);
            httpResponse.setResponseBody(responseBody);

        } catch (HttpException e) {
            exception = e;
            log.warn("executePostRequest httpException", e);
        } catch (IOException e) {
            exception = e; // 这里会捕捉到连接超时和数据返回超时异常ConnectTimeoutException，SocketTimeoutException
            log.warn("executePostRequest IOException", e);
        } catch (Exception e) {
            exception = e;
            log.warn("executePostRequest Exception", e);
        } finally {
            postMethod.releaseConnection();// 关闭连接
            if (exception != null)
                throw exception;
        }

        return httpResponse;
    }

    public static class HttpResponse {

        // http返回的状态码,比如200，404
        private int statusCode;

        // 返回的2进制结果集
        private byte[] responseBody;

        // 返回的结果k-v表
        private JSONObject resultMap;

        private boolean init;

        private String body;

        public int getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
        }

        public byte[] getResponseBody() {
            return responseBody;
        }

        public void setResponseBody(byte[] responseBody) {
            this.responseBody = responseBody;
        }

        private void initResult() {
            try {
                if (!init) {
                    body = new String(responseBody, "utf-8").trim();
                    resultMap = JSON.parseObject(body, JSONObject.class);
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } finally {
                init = true;
            }
        }

        public boolean isOk() {
            // http请求2xx表示返回成功
            return statusCode / 100 == 2;
        }

        public String getValueByKey(String key) {
            initResult();
            if (resultMap == null) {
                return null;
            }
            return resultMap.getString(key);
        }

        public Integer getIntValue(String key) {
            initResult();
            if (resultMap == null) {
                return null;
            }
            return resultMap.getInteger(key);
        }

        public boolean hasKey(String key) {
            initResult();
            if (resultMap == null) {
                return false;
            }
            return resultMap.containsKey(key);
        }

        public JSONObject getJsonObject(String key) {
            initResult();
            if (resultMap == null) {
                return null;
            }
            return resultMap.getJSONObject(key);
        }

        public JSONObject resultMap() {
            initResult();
            return resultMap;
        }

        @Override
        public String toString() {
            initResult();
            return body;
        }
    }
}
