package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName Cumulativebenefits.xlsx
 * @sheetName Cumulativebenefits
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class CumulativebenefitsCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "Cumulativebenefits.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "Cumulativebenefits";

  /** 达成条件 */
  protected int condition;
  /** 任务组 */
  protected int group;
  /** 道具ID_道具数量 */
  protected Map<Integer,Long> rewards;
  /** 类型 */
  protected int type;

  /** 返回达成条件 */
  public int getCondition() {
    return condition;
  }

  /** 返回任务组 */
  public int getGroup() {
    return group;
  }

  /** 返回道具ID_道具数量 */
  public Map<Integer,Long> getRewards() {
    return rewards;
  }

  /** 返回类型 */
  public int getType() {
    return type;
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
