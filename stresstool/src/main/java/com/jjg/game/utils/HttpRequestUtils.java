package com.jjg.game.utils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class HttpRequestUtils {

  /**
   * Get请求
   *
   * @param requestUrl 请求路径
   * @return 请求成功的数据
   */
  public static String sendGet(String requestUrl) {
    return sendGet(requestUrl, 0);
  }

  /**
   * Get请求
   *
   * @param requestUrl 请求路径
   * @param connTimeOut 请求超时时间
   * @return 请求成功的数据
   */
  public static String sendGet(String requestUrl, int connTimeOut) {
    HttpURLConnection connection = null;
    BufferedReader bufferedReader = null;
    int httpResponseCode = -1;
    try {
      URL url = new URL(requestUrl);
      connection = (HttpURLConnection) url.openConnection();
      // 设置请求属性
      connection.setRequestProperty("accept", "*/*");
      connection.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");
      connection.setRequestMethod("GET");
      connection.setRequestProperty("connection", "Keep-Alive");
      connection.setRequestProperty(
          "user-agent",
          "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.80 Safari/537.36");
      connection.setConnectTimeout(connTimeOut > 0 ? connTimeOut : 30_000);
      // 请求目标文件
      connection.connect();
      httpResponseCode = connection.getResponseCode();
      InputStream inputStream = connection.getInputStream();
      bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
      StringBuilder stringBuffer = new StringBuilder();
      String res;
      while ((res = bufferedReader.readLine()) != null) {
        stringBuffer.append(res);
      }
      return stringBuffer.toString();
    } catch (IOException ioException) {
      LoggerUtils.LOGGER.error(
          "request failed error code:"
              + httpResponseCode
              + " err msg: "
              + ioException.getMessage());
    } finally {
      if (bufferedReader != null) {
        try {
          bufferedReader.close();
        } catch (IOException ioException) {
          LoggerUtils.LOGGER.error("close error" + ioException.getMessage());
        }
      }
      if (connection != null) {
        connection.disconnect();
      }
    }
    return null;
  }

  /**
   * httpPost请求
   *
   * @param requestUrl 请求地址
   * @return 响应数据
   */
  public static String sendPost(String requestUrl) {
    return sendPost(requestUrl, "", 0, null);
  }

  /**
   * httpPost请求
   *
   * @param requestUrl 请求地址
   * @param params 请求的Map参数
   * @return 响应数据
   */
  public static String sendPost(String requestUrl, Map<String, String> params) {
    Map<String, String> messageHeaderProperty = new HashMap<>(1);
    messageHeaderProperty.put("Content-Type", "text/plain; charset=UTF-8");
    return sendPost(requestUrl, params, messageHeaderProperty);
  }

  /**
   * httpPost请求
   *
   * @param requestUrl 请求地址
   * @param params 请求的Map参数
   * @return 响应数据
   */
  public static String sendPost(
      String requestUrl, Map<String, String> params, Map<String, String> messageHeaderProperty) {
    StringBuilder sendData = new StringBuilder();
    if (params != null && !params.isEmpty()) {
      for (Map.Entry<String, String> entry : params.entrySet()) {
        sendData.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
      }
      if (!sendData.isEmpty()) {
        // 删除最后一个&符号
        sendData.setLength(sendData.length() - 1);
      }
    }
    return sendPost(requestUrl, sendData.toString(), 0, messageHeaderProperty);
  }

  /**
   * httpPost请求
   *
   * @param requestUrl 请求地址
   * @param data post数据
   * @return 响应数据
   */
  public static String sendPost(
      String requestUrl, String data, int connTimeOut, Map<String, String> messageHeaderProperty) {
    StringBuilder responseBuilder;
    BufferedReader reader = null;
    OutputStreamWriter streamWriter = null;
    int connResponseCode = -1;
    HttpURLConnection urlConnection = null;
    try {
      URL url = new URL(requestUrl);
      urlConnection = (HttpURLConnection) url.openConnection();
      // 使用文本
      if (messageHeaderProperty != null && !messageHeaderProperty.isEmpty()) {
        messageHeaderProperty.forEach(urlConnection::setRequestProperty);
      }
      urlConnection.setRequestMethod("POST");
      urlConnection.setDoOutput(true);
      // 发送post请求需添加输入数据
      urlConnection.setDoInput(true);
      urlConnection.setConnectTimeout(connTimeOut > 0 ? connTimeOut : 30_000);
      urlConnection.setReadTimeout(0);

      streamWriter = new OutputStreamWriter(urlConnection.getOutputStream());
      streamWriter.write(data);
      streamWriter.flush();

      connResponseCode = urlConnection.getResponseCode();
      reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
      responseBuilder = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        responseBuilder.append(line);
      }
      return responseBuilder.toString();
    } catch (IOException ioException) {
      LoggerUtils.LOGGER.error(
          "request failed error code:" + connResponseCode + " " + ioException.getMessage());
    } finally {
      if (streamWriter != null) {
        try {
          streamWriter.close();
        } catch (IOException ioException) {
          LoggerUtils.LOGGER.error("close error", ioException);
        }
      }

      if (reader != null) {
        try {
          reader.close();
        } catch (IOException ioException) {
          LoggerUtils.LOGGER.error("close error", ioException);
        }
      }
      if (urlConnection != null) {
        urlConnection.disconnect();
      }
    }
    return null;
  }
}
