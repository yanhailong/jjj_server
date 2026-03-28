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

  /** 任务条件_道具ID_道具数量 */
  protected Map<Integer,List<Long>> rewards;
  /** 类型 */
  protected int type;
  /** 权重 */
  protected int weight;

  /** 返回任务条件_道具ID_道具数量 */
  public Map<Integer,List<Long>> getRewards() {
    return rewards;
  }

  /** 返回类型 */
  public int getType() {
    return type;
  }

  /** 返回权重 */
  public int getWeight() {
    return weight;
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
