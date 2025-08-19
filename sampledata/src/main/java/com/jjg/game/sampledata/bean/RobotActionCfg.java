package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName RobotAction
 * @sheetName RobotAction
 * @author Auto.Generator
 * @date 2025年08月19日 15:29:42
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class RobotActionCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "RobotAction";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "RobotAction";

  /** 押注行为ID */
  protected int actionID;
  /** 是否押注 */
  protected int betAction;
  /** 押注筹码 */
  protected List<List<Integer>> betChips;
  /** 押注区域 */
  protected List<List<Integer>> bettingArea;
  /** 延迟押注时间 */
  protected List<List<Integer>> delayTime;
  /** 失败时押注筹码 */
  protected List<List<Integer>> failBetAction;
  /** 游戏ID */
  protected int gameID;
  /** 再次押注等待 */
  protected List<List<Integer>> nextTime;
  /** 获胜时押注筹码 */
  protected List<List<Integer>> winBetAction;

  /** 返回押注行为ID */
  public int getActionID() {
    return actionID;
  }

  /** 返回是否押注 */
  public int getBetAction() {
    return betAction;
  }

  /** 返回押注筹码 */
  public List<List<Integer>> getBetChips() {
    return betChips;
  }

  /** 返回押注区域 */
  public List<List<Integer>> getBettingArea() {
    return bettingArea;
  }

  /** 返回延迟押注时间 */
  public List<List<Integer>> getDelayTime() {
    return delayTime;
  }

  /** 返回失败时押注筹码 */
  public List<List<Integer>> getFailBetAction() {
    return failBetAction;
  }

  /** 返回游戏ID */
  public int getGameID() {
    return gameID;
  }

  /** 返回再次押注等待 */
  public List<List<Integer>> getNextTime() {
    return nextTime;
  }

  /** 返回获胜时押注筹码 */
  public List<List<Integer>> getWinBetAction() {
    return winBetAction;
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
