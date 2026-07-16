package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName MiningAchievement.xlsx
 * @sheetName MiningAchievement
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class MiningAchievementCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "MiningAchievement.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "MiningAchievement";

  /** 成就描述 */
  protected int AchievementDescription;
  /** 成就名称 */
  protected int AchievementName;
  /** 达成条件 */
  protected List<Long> AllianceTaskConditionId;
  /** 奖励 */
  protected Map<Integer,Long> Reward;

  /** 返回成就描述 */
  public int getAchievementDescription() {
    return AchievementDescription;
  }

  /** 返回成就名称 */
  public int getAchievementName() {
    return AchievementName;
  }

  /** 返回达成条件 */
  public List<Long> getAllianceTaskConditionId() {
    return AllianceTaskConditionId;
  }

  /** 返回奖励 */
  public Map<Integer,Long> getReward() {
    return Reward;
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
