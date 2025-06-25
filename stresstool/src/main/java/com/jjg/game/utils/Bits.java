package com.jjg.game.utils;

/** @function 字节工具类 */
public final class Bits {

  /**
   * int 转 byte数组
   *
   * @param target
   * @return
   */
  public static byte[] int2Bytes(int target) {
    byte[] intBytes = new byte[4];
    intBytes[3] = (byte) (target & 0xff); // 最低位
    intBytes[2] = (byte) (target >> 8 & 0xff); // 次低位
    intBytes[1] = (byte) (target >> 16 & 0xff); // 次高位
    intBytes[0] = (byte) (target >>> 24 & 0xff); // 最高位,无符号右移。
    return intBytes;
  }

  /**
   * long 转 byte数组
   *
   * @param target
   * @return
   */
  public static byte[] long2Bytes(long target) {
    byte[] longBytes = new byte[8];
    for (int i = 0; i < longBytes.length; i++) {
      longBytes[i] = new Long(target & 0xff).byteValue();
      target = target >> 8;
    }
    return longBytes;
  }
}
