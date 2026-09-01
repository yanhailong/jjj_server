package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName BuyOneGetSeven.xlsx
 * @sheetName BuyOneGetSeven
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BuyOneGetSevenCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "BuyOneGetSeven.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "BuyOneGetSeven";

  /** 礼包内容 */
  protected Map<Integer,Long> GetItem;

  /** 返回礼包内容 */
  public Map<Integer,Long> getGetItem() {
    return GetItem;
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
