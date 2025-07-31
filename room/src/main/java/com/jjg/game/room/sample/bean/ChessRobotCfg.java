package com.jjg.game.room.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName RobotAction_Chess.xlsx
 * @sheetName ChessRobot
 * @author Auto.Generator
 * @date 2025年07月31日 16:29:11
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ChessRobotCfg extends RobotActionCfg {

  /** 配置表名 */
  public static final String EXCEL_NAME = "RobotAction_Chess.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "ChessRobot";

  /** 押注行为ID */
  protected int ActionID;
  /** 延迟押注时间 */
  protected List<List<Integer>> DelayTime;
  /** 失败时押注筹码 */
  protected List<List<Integer>> FailBetAction;
  /** 游戏ID */
  protected int GameID;
  /** 序列 */
  protected int ID;
  /** 获胜时押注筹码 */
  protected List<List<Integer>> WinBetAction;

  /** 返回押注行为ID */
  public int getActionID() {
    return ActionID;
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

  /** 返回获胜时押注筹码 */
  public List<List<Integer>> getWinBetAction() {
    return WinBetAction;
  }
}
