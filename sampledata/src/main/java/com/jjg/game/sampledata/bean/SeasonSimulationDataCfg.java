package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName SeasonSimulationData.xlsx
 * @sheetName SeasonSimulationData
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SeasonSimulationDataCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "SeasonSimulationData.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "SeasonSimulationData";

  /** 抽取权重 */
  protected int ExtractionWeight;
  /** 模拟结果倍数 */
  protected List<Integer> Multiplier;
  /** 赛季类型 */
  protected int type;

  /** 返回抽取权重 */
  public int getExtractionWeight() {
    return ExtractionWeight;
  }

  /** 返回模拟结果倍数 */
  public List<Integer> getMultiplier() {
    return Multiplier;
  }

  /** 返回赛季类型 */
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
