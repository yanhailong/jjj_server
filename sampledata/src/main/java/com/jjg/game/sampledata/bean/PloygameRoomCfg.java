package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName PloygameRoom.xlsx
 * @sheetName PloygameRoom
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PloygameRoomCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "PloygameRoom.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "PloygameRoom";

  /** 机器人加入房间间隔（毫秒） */
  protected List<Integer> IntervalTime;
  /** 参数-税率 */
  protected int TaxRate;
  /** 默认押分 */
  protected int defaultBet;
  /** 游戏ID */
  protected int gameType;
  /** 参数-信息 */
  protected List<Integer> information;
  /** 初始化标准池 */
  protected long initBasePool;
  /** 押注进入标准池万分比 */
  protected int initBasePoolProportion;
  /** 单线押分值 */
  protected List<Integer> lineBetScore;
  /** 跑马触发金额 */
  protected List<Long> marqueeTrigger;
  /** 多语言表ID */
  protected int nameid;
  /** 参数-赔率 */
  protected Map<Integer,Integer> odds;
  /** 参数-返奖率 */
  protected int rewardRate;
  /** 机器人人数（时间段:机器人人数|……） */
  protected List<List<Integer>> robot_num;
  /** 倍场名称 */
  protected int roomName;

  /** 返回机器人加入房间间隔（毫秒） */
  public List<Integer> getIntervalTime() {
    return IntervalTime;
  }

  /** 返回参数-税率 */
  public int getTaxRate() {
    return TaxRate;
  }

  /** 返回默认押分 */
  public int getDefaultBet() {
    return defaultBet;
  }

  /** 返回游戏ID */
  public int getGameType() {
    return gameType;
  }

  /** 返回参数-信息 */
  public List<Integer> getInformation() {
    return information;
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

  /** 返回多语言表ID */
  public int getNameid() {
    return nameid;
  }

  /** 返回参数-赔率 */
  public Map<Integer,Integer> getOdds() {
    return odds;
  }

  /** 返回参数-返奖率 */
  public int getRewardRate() {
    return rewardRate;
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
