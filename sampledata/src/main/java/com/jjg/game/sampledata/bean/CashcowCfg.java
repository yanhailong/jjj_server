package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName CashCow.xlsx
 * @sheetName cashcow
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class CashcowCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "CashCow.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "cashcow";

  /** 达成条件 */
  protected int condition;
  /** 所占总奖池比例万分比 */
  protected int distribution;
  /** 奖池初始值 */
  protected int initialprizepool;
  /** 消耗数量 */
  protected String needItem;
  /** 道具ID_道具数量 */
  protected List<Integer> rewards;
  /** 类型 */
  protected int type;
  /** 次数下限_次数上限（左闭右开）_玩家中奖万分比概率| */
  protected List<List<Integer>> weight;
  /** 机器人中奖频率 */
  protected List<List<Integer>> winningFrequency;

  /** 返回达成条件 */
  public int getCondition() {
    return condition;
  }

  /** 返回所占总奖池比例万分比 */
  public int getDistribution() {
    return distribution;
  }

  /** 返回奖池初始值 */
  public int getInitialprizepool() {
    return initialprizepool;
  }

  /** 返回消耗数量 */
  public String getNeedItem() {
    return needItem;
  }

  /** 返回道具ID_道具数量 */
  public List<Integer> getRewards() {
    return rewards;
  }

  /** 返回类型 */
  public int getType() {
    return type;
  }

  /** 返回次数下限_次数上限（左闭右开）_玩家中奖万分比概率| */
  public List<List<Integer>> getWeight() {
    return weight;
  }

  /** 返回机器人中奖频率 */
  public List<List<Integer>> getWinningFrequency() {
    return winningFrequency;
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
