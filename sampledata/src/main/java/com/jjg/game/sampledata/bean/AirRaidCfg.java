package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName AirRaid.xlsx
 * @sheetName AirRaid
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class AirRaidCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "AirRaid.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "AirRaid";

  /** 系统赢抽水 */
  protected int EffectiveRatio;
  /** 倍数增长率（r） */
  protected int GrowthMultiplier;
  /** 初始坠毁率（p0） */
  protected int InitialCrashRate;
  /** 机器人加入房间间隔（毫秒） */
  protected List<Integer> IntervalTime;
  /** 风险增量（k） */
  protected int RiskIncrement;
  /** 阶段信息 */
  protected List<Integer> StageTime;
  /** 玩家赢抽水 */
  protected int WinRatio;
  /** 单线押分值 */
  protected List<Integer> betList;
  /** 游戏ID */
  protected int gameID;
  /** 跑马灯触发金额 */
  protected List<Long> marqueeTrigger;
  /** 多语言表ID */
  protected int nameid;
  /** 机器人人数（时间段:机器人人数|……） */
  protected List<List<Integer>> robot_num;

  /** 返回系统赢抽水 */
  public int getEffectiveRatio() {
    return EffectiveRatio;
  }

  /** 返回倍数增长率（r） */
  public int getGrowthMultiplier() {
    return GrowthMultiplier;
  }

  /** 返回初始坠毁率（p0） */
  public int getInitialCrashRate() {
    return InitialCrashRate;
  }

  /** 返回机器人加入房间间隔（毫秒） */
  public List<Integer> getIntervalTime() {
    return IntervalTime;
  }

  /** 返回风险增量（k） */
  public int getRiskIncrement() {
    return RiskIncrement;
  }

  /** 返回阶段信息 */
  public List<Integer> getStageTime() {
    return StageTime;
  }

  /** 返回玩家赢抽水 */
  public int getWinRatio() {
    return WinRatio;
  }

  /** 返回单线押分值 */
  public List<Integer> getBetList() {
    return betList;
  }

  /** 返回游戏ID */
  public int getGameID() {
    return gameID;
  }

  /** 返回跑马灯触发金额 */
  public List<Long> getMarqueeTrigger() {
    return marqueeTrigger;
  }

  /** 返回多语言表ID */
  public int getNameid() {
    return nameid;
  }

  /** 返回机器人人数（时间段:机器人人数|……） */
  public List<List<Integer>> getRobot_num() {
    return robot_num;
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
