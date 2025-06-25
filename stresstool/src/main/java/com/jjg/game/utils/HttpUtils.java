package com.jjg.game.utils;

import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class HttpUtils {

  private static final Log LOGGER = LogFactory.getLog(HttpUtils.class);

  /**
   * http get
   *
   * @param uri
   * @return
   */
  public static String get(String uri) {
    return get(uri, StrEx.CHARSET_UTF8);
  }

  /**
   * http get 自定编码
   *
   * @param uri
   * @param charset 字符集
   * @return
   */
  public static String get(String uri, String charset) {

    return null;
  }

  /**
   * http post
   *
   * @param uri
   * @param parameters map<String,String>
   * @return
   * @throws Exception
   */
  public static String post(String uri, Map<String, String> parameters) {
    return post(uri, parameters, StrEx.CHARSET_UTF8);
  }

  /**
   * http post
   *
   * @param uri
   * @param parameters map<String,String>
   * @param charset 字符集
   * @return
   * @throws Exception
   */
  public static String post(String uri, Map<String, String> parameters, String charset) {
      return null;
  }

  /**
   * http post
   *
   * @param uri
   * @return
   * @throws Exception
   */
  public static String post(String uri) throws Exception {
    return post(uri, null);
  }
}
