package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName AllianceTask.xlsx
 * @sheetName AllianceTask
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class AllianceTaskCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "AllianceTask.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "AllianceTask";

  /** 放弃的冷却时间（秒） */
  protected int AbandonCooldown;
  /** 任务条件 */
  protected List<Long> AllianceTaskConditionId;
  /** 任务类型 */
  protected int AllianceTasktype;
  /** 是否允许放弃 */
  protected int AllowAbandon;
  /** 贡献值奖励 */
  protected int ContributionReward;
  /** 持续时间（M) */
  protected int Duration;
  /** 任务品质 */
  protected int Quality;
  /** 声誉值奖励 */
  protected int ReputationReward;
  /** 序列 */
  protected int Sequence;
  /** 任务描述 */
  protected int language;

  /** 返回放弃的冷却时间（秒） */
  public int getAbandonCooldown() {
    return AbandonCooldown;
  }

  /** 返回任务条件 */
  public List<Long> getAllianceTaskConditionId() {
    return AllianceTaskConditionId;
  }

  /** 返回任务类型 */
  public int getAllianceTasktype() {
    return AllianceTasktype;
  }

  /** 返回是否允许放弃 */
  public int getAllowAbandon() {
    return AllowAbandon;
  }

  /** 返回贡献值奖励 */
  public int getContributionReward() {
    return ContributionReward;
  }

  /** 返回持续时间（M) */
  public int getDuration() {
    return Duration;
  }

  /** 返回任务品质 */
  public int getQuality() {
    return Quality;
  }

  /** 返回声誉值奖励 */
  public int getReputationReward() {
    return ReputationReward;
  }

  /** 返回序列 */
  public int getSequence() {
    return Sequence;
  }

  /** 返回任务描述 */
  public int getLanguage() {
    return language;
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
