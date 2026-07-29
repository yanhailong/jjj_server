package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName ImmortalHand.xlsx
 * @sheetName ImmortalHand
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ImmortalHandCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "ImmortalHand.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "ImmortalHand";

  /** 区域 */
  protected int Area;
  /** 牌型倍率 */
  protected int HandMultiplier;
  /** 牌型值 */
  protected int HandValue;
  /** 多语言id */
  protected int nameid;

  /** 返回区域 */
  public int getArea() {
    return Area;
  }

  /** 返回牌型倍率 */
  public int getHandMultiplier() {
    return HandMultiplier;
  }

  /** 返回牌型值 */
  public int getHandValue() {
    return HandValue;
  }

  /** 返回多语言id */
  public int getNameid() {
    return nameid;
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
