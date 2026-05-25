package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName CasinoList.xlsx
 * @sheetName CasinoList
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class CasinoListCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "CasinoList.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "CasinoList";

  /** 城市名称 */
  protected int cityname;
  /** 解锁条件 */
  protected Map<Integer,Integer> condition;

  /** 返回城市名称 */
  public int getCityname() {
    return cityname;
  }

  /** 返回解锁条件 */
  public Map<Integer,Integer> getCondition() {
    return condition;
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
