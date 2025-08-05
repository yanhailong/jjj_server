package com.jjg.game.room.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName RobotAction
 * @sheetName RobotAction
 * @author Auto.Generator
 * @date 2025年08月05日 10:45:14
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class RobotActionCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "RobotAction";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "RobotAction";

  /** 押注行为ID */
  protected int ActionID;
  /** 是否押注 */
  protected int BetAction;
  /** 押注筹码 */
  protected List<List<Integer>> BetChips;
  /** 押注区域 */
  protected List<List<Integer>> BettingArea;
  /** 延迟押注时间 */
  protected List<List<Integer>> DelayTime;
  /** 失败时押注筹码 */
  protected List<List<Integer>> FailBetAction;
  /** 游戏ID */
  protected int GameID;
  /** 序列 */
  protected int ID;
  /** 再次押注等待 */
  protected List<List<Integer>> NextTime;
  /** 获胜时押注筹码 */
  protected List<List<Integer>> WinBetAction;

  /** 返回押注行为ID */
  public int getActionID() {
    return ActionID;
  }

  /** 返回是否押注 */
  public int getBetAction() {
    return BetAction;
  }

  /** 返回押注筹码 */
  public List<List<Integer>> getBetChips() {
    return BetChips;
  }

  /** 返回押注区域 */
  public List<List<Integer>> getBettingArea() {
    return BettingArea;
  }

  /** 返回延迟押注时间 */
  public List<List<Integer>> getDelayTime() {
    return DelayTime;
  }

  /** 返回失败时押注筹码 */
  public List<List<Integer>> getFailBetAction() {
    return FailBetAction;
  }

  /** 返回游戏ID */
  public int getGameID() {
    return GameID;
  }

  /** 返回序列 */
  public int getID() {
    return ID;
  }

  /** 返回再次押注等待 */
  public List<List<Integer>> getNextTime() {
    return NextTime;
  }

  /** 返回获胜时押注筹码 */
  public List<List<Integer>> getWinBetAction() {
    return WinBetAction;
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
