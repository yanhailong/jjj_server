package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName Room_Chess.xlsx
 * @sheetName Room_Chess
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class Room_ChessCfg extends RoomCfg {

  /** 配置表名 */
  public static final String EXCEL_NAME = "Room_Chess.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "Room_Chess";

  /** 系统抽水 */
  protected int EffectiveRatio;
  /** 机器人加入房间间隔（毫秒） */
  protected List<Integer> IntervalTime;
  /** 底注 */
  protected int betBase;
  /** 阶段信息 */
  protected Map<Integer,Integer> chess_stageOrder;
  /** 游戏ID */
  protected int gameID;
  /** 玩家手牌数量 */
  protected int handPoker;
  /** 跑马触发金额 */
  protected List<Long> marqueeTrigger;
  /** 最多人数 */
  protected int maxPlayer;
  /** 上庄最低金额 */
  protected List<Integer> minBankerAmount;
  /** 最少人数 */
  protected int minPlayer;
  /** 多语言表ID */
  protected int nameid;
  /** 玩法类型 */
  protected String playType;
  /** 机器人人数（时间段:机器人人数|……） */
  protected List<List<Integer>> robot_num;
  /** 房间类型 */
  protected int roomID;

  /** 返回系统抽水 */
  public int getEffectiveRatio() {
    return EffectiveRatio;
  }

  /** 返回机器人加入房间间隔（毫秒） */
  public List<Integer> getIntervalTime() {
    return IntervalTime;
  }

  /** 返回底注 */
  public int getBetBase() {
    return betBase;
  }

  /** 返回阶段信息 */
  public Map<Integer,Integer> getChess_stageOrder() {
    return chess_stageOrder;
  }

  /** 返回游戏ID */
  public int getGameID() {
    return gameID;
  }

  /** 返回玩家手牌数量 */
  public int getHandPoker() {
    return handPoker;
  }

  /** 返回跑马触发金额 */
  public List<Long> getMarqueeTrigger() {
    return marqueeTrigger;
  }

  /** 返回最多人数 */
  public int getMaxPlayer() {
    return maxPlayer;
  }

  /** 返回上庄最低金额 */
  public List<Integer> getMinBankerAmount() {
    return minBankerAmount;
  }

  /** 返回最少人数 */
  public int getMinPlayer() {
    return minPlayer;
  }

  /** 返回多语言表ID */
  public int getNameid() {
    return nameid;
  }

  /** 返回玩法类型 */
  public String getPlayType() {
    return playType;
  }

  /** 返回机器人人数（时间段:机器人人数|……） */
  public List<List<Integer>> getRobot_num() {
    return robot_num;
  }

  /** 返回房间类型 */
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
