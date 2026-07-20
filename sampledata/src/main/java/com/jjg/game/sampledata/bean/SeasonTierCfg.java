package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName SeasonTier.xlsx
 * @sheetName SeasonTier
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SeasonTierCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "SeasonTier.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "SeasonTier";

  /** 段位多语言 */
  protected int Rank;
  /** 段位是否降级 */
  protected boolean RankDemoted;
  /** 赛季币段位区间 */
  protected List<Integer> RankRange;
  /** 晋升奖励 */
  protected Map<Integer,Long> RankUpReward;
  /** 赛季结算可获得的徽章 */
  protected int SeasonBadge;
  /** 结算奖励 */
  protected Map<Integer,Long> SettlementReward;
  /** 段位类型 */
  protected int ranktype;

  /** 返回段位多语言 */
  public int getRank() {
    return Rank;
  }

  /** 返回段位是否降级 */
  public boolean getRankDemoted() {
    return RankDemoted;
  }

  /** 返回赛季币段位区间 */
  public List<Integer> getRankRange() {
    return RankRange;
  }

  /** 返回晋升奖励 */
  public Map<Integer,Long> getRankUpReward() {
    return RankUpReward;
  }

  /** 返回赛季结算可获得的徽章 */
  public int getSeasonBadge() {
    return SeasonBadge;
  }

  /** 返回结算奖励 */
  public Map<Integer,Long> getSettlementReward() {
    return SettlementReward;
  }

  /** 返回段位类型 */
  public int getRanktype() {
    return ranktype;
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
