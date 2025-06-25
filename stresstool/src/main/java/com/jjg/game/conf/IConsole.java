/*
 * To change this license header, choose License Headers in Project Properties. To change this
 * template file, choose Tools | Templates and open the template in the editor.
 */
package com.jjg.game.conf;

/** @author 2CL */
public interface IConsole {

  /**
   * 设置debug开关
   *
   * @param _debug
   */
  public void setDebug(boolean _debug);

  /**
   * 控制台debug开关
   *
   * @return
   */
  public boolean isDebug();

  /**
   * 设置滚屏开关
   *
   * @param _scroll
   */
  public void setScroll(boolean _scroll);

  /**
   * 控制台滚屏开关
   *
   * @return
   */
  public boolean isScroll();

  /**
   * 设置增量开关
   *
   * @param _incre
   */
  public void setIncre(boolean _incre);

  /**
   * 控制台增量开关
   *
   * @return
   */
  public boolean isIncre();

  /**
   * 设置登录开关
   *
   * @param _LoginQue
   */
  public void setLoginQue(boolean _LoginQue);

  /**
   * 控制台排队登录开关
   *
   * @return
   */
  public boolean isLoginQue();

  /**
   * 添加日志
   *
   * @param actionMsg
   * @param isDebug
   */
  public void addConsoleAreaInfo(String actionMsg, boolean isDebug);
}
