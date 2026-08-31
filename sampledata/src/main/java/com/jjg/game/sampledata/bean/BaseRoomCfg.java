package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName BaseRoom.xlsx
 * @sheetName BaseRoom
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BaseRoomCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "BaseRoom.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "BaseRoom";

  /** 机器人金币增长速率 */
  protected int GrowthRate;
  /** 间隔时间 */
  protected List<Integer> Interval;
  /** 机器人加入房间间隔（毫秒） */
  protected List<Integer> IntervalTime;
  /** 下注金额 */
  protected Map<Integer,Integer> RobotBet;
  /** 中奖倍数 */
  protected Map<Integer,Integer> WinMultiplier;
  /** 实际押注抽水入奖池万分比 */
  protected int commissionProp;
  /** 默认押分 */
  protected List<Integer> defaultBet;
  /** 假累计奖池部分进入比率 */
  protected List<Integer> fakeCommissionProp;
  /** 假总奖池金额 */
  protected long fakePool;
  /** 游戏ID */
  protected int gameType;
  /** 初始化标准池 */
  protected long initBasePool;
  /** 押注进入标准池万分比 */
  protected int initBasePoolProportion;
  /** 单线押分值 */
  protected List<Integer> lineBetScore;
  /** 跑马触发金额 */
  protected List<Long> marqueeTrigger;
  /** 准备金最低金额 */
  protected List<Integer> minBankerAmount;
  /** 多语言表ID */
  protected int nameid;
  /** 机器人人数（时间段:机器人人数|……） */
  protected List<List<Integer>> robot_num;
  /** 倍场名称 */
  protected int roomName;

  /** 返回机器人金币增长速率 */
  public int getGrowthRate() {
    return GrowthRate;
  }

  /** 返回间隔时间 */
  public List<Integer> getInterval() {
    return Interval;
  }

  /** 返回机器人加入房间间隔（毫秒） */
  public List<Integer> getIntervalTime() {
    return IntervalTime;
  }

  /** 返回下注金额 */
  public Map<Integer,Integer> getRobotBet() {
    return RobotBet;
  }

  /** 返回中奖倍数 */
  public Map<Integer,Integer> getWinMultiplier() {
    return WinMultiplier;
  }

  /** 返回实际押注抽水入奖池万分比 */
  public int getCommissionProp() {
    return commissionProp;
  }

  /** 返回默认押分 */
  public List<Integer> getDefaultBet() {
    return defaultBet;
  }

  /** 返回假累计奖池部分进入比率 */
  public List<Integer> getFakeCommissionProp() {
    return fakeCommissionProp;
  }

  /** 返回假总奖池金额 */
  public long getFakePool() {
    return fakePool;
  }

  /** 返回游戏ID */
  public int getGameType() {
    return gameType;
  }

  /** 返回初始化标准池 */
  public long getInitBasePool() {
    return initBasePool;
  }

  /** 返回押注进入标准池万分比 */
  public int getInitBasePoolProportion() {
    return initBasePoolProportion;
  }

  /** 返回单线押分值 */
  public List<Integer> getLineBetScore() {
    return lineBetScore;
  }

  /** 返回跑马触发金额 */
  public List<Long> getMarqueeTrigger() {
    return marqueeTrigger;
  }

  /** 返回准备金最低金额 */
  public List<Integer> getMinBankerAmount() {
    return minBankerAmount;
  }

  /** 返回多语言表ID */
  public int getNameid() {
    return nameid;
  }

  /** 返回机器人人数（时间段:机器人人数|……） */
  public List<List<Integer>> getRobot_num() {
    return robot_num;
  }

  /** 返回倍场名称 */
  public int getRoomName() {
    return roomName;
  }

  @Override
  public boolean equals(Object o) {
    return super.equals(o);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
