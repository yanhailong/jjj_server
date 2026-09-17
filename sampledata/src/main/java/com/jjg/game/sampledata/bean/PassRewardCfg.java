package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName PassReward.xlsx
 * @sheetName PassReward
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PassRewardCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "PassReward.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "PassReward";

  /** 需要的活跃积分 */
  protected List<Integer> ActivePoints;
  /** 初级付费奖励 */
  protected Map<Integer,Long> BasicPaidRewards;
  /** 免费奖励 */
  protected Map<Integer,Long> FreeRewards;
  /** 高级付费奖励 */
  protected Map<Integer,Long> PremiumPaidRewards;
  /** 奖励组 */
  protected int RewardGroup;
  /** 等级 */
  protected int level;

  /** 返回需要的活跃积分 */
  public List<Integer> getActivePoints() {
    return ActivePoints;
  }

  /** 返回初级付费奖励 */
  public Map<Integer,Long> getBasicPaidRewards() {
    return BasicPaidRewards;
  }

  /** 返回免费奖励 */
  public Map<Integer,Long> getFreeRewards() {
    return FreeRewards;
  }

  /** 返回高级付费奖励 */
  public Map<Integer,Long> getPremiumPaidRewards() {
    return PremiumPaidRewards;
  }

  /** 返回奖励组 */
  public int getRewardGroup() {
    return RewardGroup;
  }

  /** 返回等级 */
  public int getLevel() {
    return level;
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
