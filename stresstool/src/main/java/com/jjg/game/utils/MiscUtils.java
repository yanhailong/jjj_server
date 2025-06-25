package com.jjg.game.utils;

/**
 * 工具
 *
 * @author 2CL
 */
public class MiscUtils {

  /**
   * 通过检查“ideDebug”系统属性来确定当前环境是否为IDE环境。
   *
   * @return 如果“ideDebug”系统属性设置为“true”，则为true，否则为false
   */
  public static boolean isIdeEnvironment() {
    String val = System.getProperty("ideDebug");
    return "true".equals(val);
  }
}
