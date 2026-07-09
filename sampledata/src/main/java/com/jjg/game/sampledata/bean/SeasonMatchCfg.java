package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName SeasonMatch.xlsx
 * @sheetName SeasonMatch
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SeasonMatchCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "SeasonMatch.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "SeasonMatch";

  /** 达到所输金额后主动匹配是否受降低匹配概率影响 */
  protected boolean ActiveMatchAffected;
  /** 超过赢取上限后的下注量 */
  protected int BetAmount;
  /** 每日匹配的次数 */
  protected int DailyMatchLimit;
  /** 每日匹配所输金额的上限 */
  protected int DailyMatchLossLimit;
  /** 每日赢取的金额上限 */
  protected int DailyWinLimit;
  /** 所输金额超额后被匹配的概率 */
  protected List<List<Integer>> ExceedLossMatcProb;
  /** 匹配的冷却时间（s) */
  protected int MatchCD;
  /** 所能达到最大的下注额 */
  protected int MaxBet;
  /** 超过赢取上限后的收益比例(万分比） */
  protected List<List<Integer>> ProfitRatio;
  /** 天数 */
  protected int days;

  /** 返回达到所输金额后主动匹配是否受降低匹配概率影响 */
  public boolean getActiveMatchAffected() {
    return ActiveMatchAffected;
  }

  /** 返回超过赢取上限后的下注量 */
  public int getBetAmount() {
    return BetAmount;
  }

  /** 返回每日匹配的次数 */
  public int getDailyMatchLimit() {
    return DailyMatchLimit;
  }

  /** 返回每日匹配所输金额的上限 */
  public int getDailyMatchLossLimit() {
    return DailyMatchLossLimit;
  }

  /** 返回每日赢取的金额上限 */
  public int getDailyWinLimit() {
    return DailyWinLimit;
  }

  /** 返回所输金额超额后被匹配的概率 */
  public List<List<Integer>> getExceedLossMatcProb() {
    return ExceedLossMatcProb;
  }

  /** 返回匹配的冷却时间（s) */
  public int getMatchCD() {
    return MatchCD;
  }

  /** 返回所能达到最大的下注额 */
  public int getMaxBet() {
    return MaxBet;
  }

  /** 返回超过赢取上限后的收益比例(万分比） */
  public List<List<Integer>> getProfitRatio() {
    return ProfitRatio;
  }

  /** 返回天数 */
  public int getDays() {
    return days;
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
