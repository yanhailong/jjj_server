package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName Room_Bet.xlsx
 * @sheetName Room_Bet
 * @author Auto.Generator
 * @date 2025年08月15日 17:50:22
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class Room_BetCfg extends RoomCfg {

  /** 配置表名 */
  public static final String EXCEL_NAME = "Room_Bet.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "Room_Bet";

  /** 系统抽水万分比 */
  protected int EffectiveRatio;
  /** 不操作退出倒计时秒 */
  protected int EscTime;
  /** 踢出房间提示 */
  protected int EscTipText;
  /** 机器人加入房间间隔（毫秒） */
  protected List<Integer> IntervalTime;
  /** 历史记录清除方式 */
  protected int RecordDeleteType;
  /** 阶段信息 */
  protected List<Integer> StageTime;
  /** 不操作提示 */
  protected int TipText;
  /** 不操作提示倒计时秒 */
  protected int WaitTime;
  /** 中奖时扣除比例万分比 */
  protected int WinRatio;
  /** 押分筹码列表 */
  protected List<Integer> betList;
  /** 游戏ID */
  protected int gameID;
  /** 跑马触发金额 */
  protected List<Long> marqueeTrigger;
  /** 上庄最低金额 */
  protected List<Integer> minBankerAmount;
  /** 多语言表ID */
  protected int nameid;
  /** 当前房间显示最大记录数量 */
  protected int records_num;
  /** 结果类型 */
  protected int resultType;
  /** 机器人人数（时间段:机器人人数|……） */
  protected List<List<Integer>> robot_num;
  /** 倍场ID */
  protected int roomID;

  /** 返回系统抽水万分比 */
  public int getEffectiveRatio() {
    return EffectiveRatio;
  }

  /** 返回不操作退出倒计时秒 */
  public int getEscTime() {
    return EscTime;
  }

  /** 返回踢出房间提示 */
  public int getEscTipText() {
    return EscTipText;
  }

  /** 返回机器人加入房间间隔（毫秒） */
  public List<Integer> getIntervalTime() {
    return IntervalTime;
  }

  /** 返回历史记录清除方式 */
  public int getRecordDeleteType() {
    return RecordDeleteType;
  }

  /** 返回阶段信息 */
  public List<Integer> getStageTime() {
    return StageTime;
  }

  /** 返回不操作提示 */
  public int getTipText() {
    return TipText;
  }

  /** 返回不操作提示倒计时秒 */
  public int getWaitTime() {
    return WaitTime;
  }

  /** 返回中奖时扣除比例万分比 */
  public int getWinRatio() {
    return WinRatio;
  }

  /** 返回押分筹码列表 */
  public List<Integer> getBetList() {
    return betList;
  }

  /** 返回游戏ID */
  public int getGameID() {
    return gameID;
  }

  /** 返回跑马触发金额 */
  public List<Long> getMarqueeTrigger() {
    return marqueeTrigger;
  }

  /** 返回上庄最低金额 */
  public List<Integer> getMinBankerAmount() {
    return minBankerAmount;
  }

  /** 返回多语言表ID */
  public int getNameid() {
    return nameid;
  }

  /** 返回当前房间显示最大记录数量 */
  public int getRecords_num() {
    return records_num;
  }

  /** 返回结果类型 */
  public int getResultType() {
    return resultType;
  }

  /** 返回机器人人数（时间段:机器人人数|……） */
  public List<List<Integer>> getRobot_num() {
    return robot_num;
  }

  /** 返回倍场ID */
  public int getRoomID() {
    return roomID;
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
