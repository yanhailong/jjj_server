package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName BaseElementReward.xlsx
 * @sheetName BaseElementReward
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BaseElementRewardCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "BaseElementReward.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "BaseElementReward";

  /** 倍率 */
  protected int bet;
  /** 额外出现元素导致倍率加成{元素ID_数量_倍率} */
  protected List<List<Integer>> betTimes;
  /** 元素ID_相同元素_相同元素 */
  protected List<Integer> elementId;
  /** 触发小游戏ID */
  protected List<Integer> featureTriggerId;
  /** 游戏ID */
  protected int gameType;
  /** jackpotID */
  protected int jackpotID;
  /** 中奖条件 */
  protected int lineType;
  /** 条件参数 */
  protected int rewardNum;
  /** 额外元素倍率计算方式 */
  protected int statisticalType;

  /** 返回倍率 */
  public int getBet() {
    return bet;
  }

  /** 返回额外出现元素导致倍率加成{元素ID_数量_倍率} */
  public List<List<Integer>> getBetTimes() {
    return betTimes;
  }

  /** 返回元素ID_相同元素_相同元素 */
  public List<Integer> getElementId() {
    return elementId;
  }

  /** 返回触发小游戏ID */
  public List<Integer> getFeatureTriggerId() {
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

  /** 返回中奖条件 */
  public int getLineType() {
    return lineType;
  }

  /** 返回条件参数 */
  public int getRewardNum() {
    return rewardNum;
  }

  /** 返回额外元素倍率计算方式 */
  public int getStatisticalType() {
    return statisticalType;
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
