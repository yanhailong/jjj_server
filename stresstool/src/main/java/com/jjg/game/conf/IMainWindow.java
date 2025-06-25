/*
 * To change this license header, choose License Headers in Project Properties. To change this
 * template file, choose Tools | Templates and open the template in the editor.
 */
package com.jjg.game.conf;

/**
 * @author 2CL
 */
public interface IMainWindow {

  /** 获取运行时配置 */
  RunConf getRunConf();

  /** 获取控制台实例 */
  IConsole getConsole();

  String getSelectGroup();

  /** 是否在运行中 */
  boolean isRunning();

  /** 是否强制暂停 */
  boolean isPause();

  /** 是否强制不限次数 */
  boolean isUnLimitTimes();

  /** 功能执行的最小轮数 */
  int getMinRoundNumber();

  /** 功能执行的最大轮数 */
  int getMaxRoundNumber();

  /** 设置运行状态 */
  void setRunning(boolean running);

  /** 获取运行上下文 */
  RunContext getCtx();

  /** 初始化机器人配置 */
  void initRobot();

  /** 开始运行机器人 */
  void onStart();

  /** 停止运行机器人 */
  void onStop();

  /** 获取压测类型 1 message 2 function */
  int getStressTestType();

  /** 获取压测消息列表 */
  String getMessageId();

  /** 获取压测消息列表 */
  String getMessageInfo();

  /** 从启动参数加载 */
  void initFromArgv(String[] argv);

  /** 从配置文件加载 */
  void initFromXml(String path);

  /** 是否是本地登录 */
  boolean isLocalLogin();
}
