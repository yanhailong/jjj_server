package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName task.xlsx
 * @sheetName task
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class TaskCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "task.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "task";

  /** 放弃的冷却时间（秒） */
  protected int AbandonCooldown;
  /** 是否允许放弃 */
  protected boolean AllowAbandon;
  /** 持续时间（分） */
  protected int Duration;
  /** 解锁功能ID */
  protected int FunctionId;
  /** 任务人数上下限 */
  protected List<Integer> MinandMax;
  /** 任务品质 */
  protected int Quality;
  /** 道具奖励 */
  protected Map<Integer,Long> getItem;
  /** 任务组 */
  protected int group;
  /** 积分ICON */
  protected String integralIcon;
  /** 积分奖励 */
  protected int integralNum;
  /** 按钮跳转类型 */
  protected int jumpType;
  /** 任务描述 */
  protected int language;
  /** 任务条件 */
  protected List<Long> taskConditionId;
  /** 任务图标 */
  protected String taskIcon;
  /** 任务类型 */
  protected int taskType;
  /** 开启时间 */
  protected String time;

  /** 返回放弃的冷却时间（秒） */
  public int getAbandonCooldown() {
    return AbandonCooldown;
  }

  /** 返回是否允许放弃 */
  public boolean getAllowAbandon() {
    return AllowAbandon;
  }

  /** 返回持续时间（分） */
  public int getDuration() {
    return Duration;
  }

  /** 返回解锁功能ID */
  public int getFunctionId() {
    return FunctionId;
  }

  /** 返回任务人数上下限 */
  public List<Integer> getMinandMax() {
    return MinandMax;
  }

  /** 返回任务品质 */
  public int getQuality() {
    return Quality;
  }

  /** 返回道具奖励 */
  public Map<Integer,Long> getGetItem() {
    return getItem;
  }

  /** 返回任务组 */
  public int getGroup() {
    return group;
  }

  /** 返回积分ICON */
  public String getIntegralIcon() {
    return integralIcon;
  }

  /** 返回积分奖励 */
  public int getIntegralNum() {
    return integralNum;
  }

  /** 返回按钮跳转类型 */
  public int getJumpType() {
    return jumpType;
  }

  /** 返回任务描述 */
  public int getLanguage() {
    return language;
  }

  /** 返回任务条件 */
  public List<Long> getTaskConditionId() {
    return taskConditionId;
  }

  /** 返回任务图标 */
  public String getTaskIcon() {
    return taskIcon;
  }

  /** 返回任务类型 */
  public int getTaskType() {
    return taskType;
  }

  /** 返回开启时间 */
  public String getTime() {
    return time;
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
