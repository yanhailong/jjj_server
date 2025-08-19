package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName BaseElementReward.xlsx
 * @sheetName BaseElementReward
 * @author Auto.Generator
 * @date 2025年08月19日 11:30:38
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BaseElementRewardCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "BaseElementReward.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "BaseElementReward";

  /** 倍率 */
  protected int bet;
  /** 元素倍率加成 */
  protected List<List<Integer>> betTimes;
  /** 元素ID */
  protected int elementId;
  /** 触发小游戏ID */
  protected Map<Integer,Integer> featureTriggerId;
  /** 游戏ID */
  protected int gameType;
  /** jackpotID */
  protected int jackpotID;
  /** 线ID加成 */
  protected List<List<Integer>> lineIdReward;
  /** 线类型 */
  protected int lineType;
  /** 是否额外投注 */
  protected boolean needExtraBet;
  /** 数量 */
  protected int rewardNum;
  /** 中奖基数 */
  protected int rewardType;
  /** 旋转状态 */
  protected int rotateState;
  /** 单线押分倍数加成 */
  protected List<List<Integer>> scoreBetTimes;
  /** 旋转状态标识 */
  protected int spinType;

  /** 返回倍率 */
  public int getBet() {
    return bet;
  }

  /** 返回元素倍率加成 */
  public List<List<Integer>> getBetTimes() {
    return betTimes;
  }

  /** 返回元素ID */
  public int getElementId() {
    return elementId;
  }

  /** 返回触发小游戏ID */
  public Map<Integer,Integer> getFeatureTriggerId() {
    return featureTriggerId;
  }

  /** 返回游戏ID */
  public int getGameType() {
    return gameType;
  }

  /** 返回jackpotID */
  public int getJackpotID() {
    return jackpotID;
  }

  /** 返回线ID加成 */
  public List<List<Integer>> getLineIdReward() {
    return lineIdReward;
  }

  /** 返回线类型 */
  public int getLineType() {
    return lineType;
  }

  /** 返回是否额外投注 */
  public boolean getNeedExtraBet() {
    return needExtraBet;
  }

  /** 返回数量 */
  public int getRewardNum() {
    return rewardNum;
  }

  /** 返回中奖基数 */
  public int getRewardType() {
    return rewardType;
  }

  /** 返回旋转状态 */
  public int getRotateState() {
    return rotateState;
  }

  /** 返回单线押分倍数加成 */
  public List<List<Integer>> getScoreBetTimes() {
    return scoreBetTimes;
  }

  /** 返回旋转状态标识 */
  public int getSpinType() {
    return spinType;
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
