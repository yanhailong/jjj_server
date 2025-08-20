package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName RobotAction_Chess.xlsx
 * @sheetName ChessRobot
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ChessRobotCfg extends RobotActionCfg {

  /** 配置表名 */
  public static final String EXCEL_NAME = "RobotAction_Chess.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "ChessRobot";

  /** 押注行为ID */
  protected int actionID;
  /** 延迟押注时间 */
  protected List<List<Integer>> delayTime;
  /** 失败时押注筹码 */
  protected List<List<Integer>> failBetAction;
  /** 游戏ID */
  protected int gameID;
  /** 获胜时押注筹码 */
  protected List<List<Integer>> winBetAction;

  /** 返回押注行为ID */
  public int getActionID() {
    return actionID;
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
