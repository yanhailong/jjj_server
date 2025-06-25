/*
 * To change this license header, choose License Headers in Project Properties. To change this
 * template file, choose Tools | Templates and open the template in the editor.
 */
package com.jjg.game.conf;

import java.util.concurrent.atomic.AtomicLong;

import com.jjg.game.core.robot.RobotThread;

/**
 * 压测上下文
 *
 * @author 2CL
 */
public class RunContext {

  /** 一堆计数器 * */
  protected final AtomicLong nowConnections = new AtomicLong();

  protected final AtomicLong nowLogined = new AtomicLong();
  protected final AtomicLong totConnections = new AtomicLong();
  protected final AtomicLong logicSendMsgs = new AtomicLong();
  protected final AtomicLong completeSendMsgs = new AtomicLong();
  protected final AtomicLong receiveMsgs = new AtomicLong();

  public void setAlltoZero() {
    nowConnections.set(0);
    nowLogined.set(0);
    totConnections.set(0);
    logicSendMsgs.set(0);
    completeSendMsgs.set(0);
    receiveMsgs.set(0);
  }

  public long getNowConnections() {
    return nowConnections.get();
  }

  /** 建立新的连接 */
  public void addConnection() {
    nowConnections.incrementAndGet();
  }

  public void addConnectionTots() {
    totConnections.incrementAndGet();
  }

  public long getConnectionTots() {
    return totConnections.get();
  }

  public void addLogined() {
    nowLogined.incrementAndGet();
  }

  public long getLogined() {
    return nowLogined.get();
  }

  /**
   * 连接断开
   *
   * @param robot
   */
  public void closeConnection(RobotThread robot) {
    nowConnections.decrementAndGet();
    if (robot.getPlayer().getIsLogin()) {
      nowLogined.decrementAndGet();
    }
  }

  /** 逻辑发送消息数据量 */
  public void addSendLogicMsgs() {
    logicSendMsgs.incrementAndGet();
  }

  /** 逻辑发送消息数据量 */
  public long getSendLogicMsgs() {
    return logicSendMsgs.get();
  }

  /** 刷新发送成功消息数据量 */
  public void addCompleteSendMsgs() {
    completeSendMsgs.incrementAndGet();
  }

  /** 获取发送成功消息数据量 */
  public long getSendMsgs() {
    return completeSendMsgs.get();
  }

  /** 刷新接收消息数据量 */
  public void addReceiveMsgs() {
    receiveMsgs.incrementAndGet();
  }

  /** 刷新接收消息数据量 */
  public long getReceiveMsgs() {
    return receiveMsgs.get();
  }
}
