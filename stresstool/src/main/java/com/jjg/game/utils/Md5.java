package com.jjg.game.utils;

import java.security.MessageDigest;

/**
 * @author 2CL
 */
public class Md5 {
  public static String encode(String s) {
    byte[] b = s.getBytes();
    return getMd5(b);
  }

  public static String getBytes(byte[] v) {
    return getMd5(v);
  }

  private static String getMd5(byte[] v) {
    char[] hexDigits = {
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };
    try {
      MessageDigest mdAlgorithm = MessageDigest.getInstance("MD5");
      mdAlgorithm.update(v);
      byte[] mdCode = mdAlgorithm.digest();

      int mdCodeLength = mdCode.length;
      char[] strMd5 = new char[mdCodeLength * 2];
      int k = 0;
      for (byte byte0 : mdCode) {
        strMd5[k++] = hexDigits[byte0 >>> 4 & 0xf];
        strMd5[k++] = hexDigits[byte0 & 0xf];
      }
      return new String(strMd5);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
      return "";
    }
  }
}
