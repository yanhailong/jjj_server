package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName OfficialAwards.xlsx
 * @sheetName OfficialAwards
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class OfficialAwardsCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "OfficialAwards.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "OfficialAwards";

  /** 奖励 */
  protected Map<Integer,Long> getItem;
  /** 中奖概率(万分比) */
  protected int probability;

  /** 返回奖励 */
  public Map<Integer,Long> getGetItem() {
    return getItem;
  }

  /** 返回中奖概率(万分比) */
  public int getProbability() {
    return probability;
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
