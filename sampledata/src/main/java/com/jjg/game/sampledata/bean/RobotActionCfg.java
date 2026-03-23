package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName RobotAction
 * @sheetName RobotAction
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class RobotActionCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "RobotAction";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "RobotAction";

  /** 押注行为ID */
  protected int actionID;
  /** 德州-加注时大盲筹码倍数权重 */
  protected Map<Integer,Integer> addBetMultiple;
  /** 德州-【加注】权重乘值万分比 */
  protected int addProactiveWeight;
  /** 德州-最后一轮最大牌且加注时Allin概率万分比 */
  protected int allinWeight;
  /** 是否押注 */
  protected int betAction;
  /** 押注筹码 */
  protected List<List<Integer>> betChips;
  /** 押注区域 */
  protected List<List<Integer>> bettingArea;
  /** 21点-筹码下注权重(筹码位置_权重） */
  protected Map<Integer,Integer> blackjackBet;
  /** 失败后行为概率,准备概率_展示牌型 */
  protected List<Integer> continueAfterFail;
  /** 胜利后行为概率,准备概率_展示牌型 */
  protected List<Integer> continueAfterVictory;
  /** 每次延迟行为时间 */
  protected List<List<Integer>> delayTime;
  /** 游戏ID */
  protected int gameID;
  /** 再次押注等待 */
  protected List<List<Integer>> nextTime;
  /** 21点-【要牌】权重乘值万分比 */
  protected int standWeight;

  /** 返回押注行为ID */
  public int getActionID() {
    return actionID;
  }

  /** 返回德州-加注时大盲筹码倍数权重 */
  public Map<Integer,Integer> getAddBetMultiple() {
    return addBetMultiple;
  }

  /** 返回德州-【加注】权重乘值万分比 */
  public int getAddProactiveWeight() {
    return addProactiveWeight;
  }

  /** 返回德州-最后一轮最大牌且加注时Allin概率万分比 */
  public int getAllinWeight() {
    return allinWeight;
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

  /** 返回21点-筹码下注权重(筹码位置_权重） */
  public Map<Integer,Integer> getBlackjackBet() {
    return blackjackBet;
  }

  /** 返回失败后行为概率,准备概率_展示牌型 */
  public List<Integer> getContinueAfterFail() {
    return continueAfterFail;
  }

  /** 返回胜利后行为概率,准备概率_展示牌型 */
  public List<Integer> getContinueAfterVictory() {
    return continueAfterVictory;
  }

  /** 返回每次延迟行为时间 */
  public List<List<Integer>> getDelayTime() {
    return delayTime;
  }

  /** 返回游戏ID */
  public int getGameID() {
    return gameID;
  }

  /** 返回再次押注等待 */
  public List<List<Integer>> getNextTime() {
    return nextTime;
  }

  /** 返回21点-【要牌】权重乘值万分比 */
  public int getStandWeight() {
    return standWeight;
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
