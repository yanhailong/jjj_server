package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName GiftList.xlsx
 * @sheetName GiftList
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class GiftListCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "GiftList.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "GiftList";

  /** 增加人气值 */
  protected int Popularity;
  /** 购买价格 */
  protected Map<Integer,Long> cost;
  /** 客户端资源 */
  protected String icon;
  /** 名称多语言ID */
  protected int name;

  /** 返回增加人气值 */
  public int getPopularity() {
    return Popularity;
  }

  /** 返回购买价格 */
  public Map<Integer,Long> getCost() {
    return cost;
  }

  /** 返回客户端资源 */
  public String getIcon() {
    return icon;
  }

  /** 返回名称多语言ID */
  public int getName() {
    return name;
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
