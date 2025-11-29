package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName RouletteShop.xlsx
 * @sheetName RouletteShop
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class RouletteShopCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "RouletteShop.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "RouletteShop";

  /** 每日限购次数 */
  protected int Frequency;
  /** 商店表序列 */
  protected int Sequence;
  /** 购买消耗 */
  protected List<Integer> Value;
  /** 商品奖励 */
  protected List<Integer> item;
  /** 是否开启 */
  protected int open;

  /** 返回每日限购次数 */
  public int getFrequency() {
    return Frequency;
  }

  /** 返回商店表序列 */
  public int getSequence() {
    return Sequence;
  }

  /** 返回购买消耗 */
  public List<Integer> getValue() {
    return Value;
  }

  /** 返回商品奖励 */
  public List<Integer> getItem() {
    return item;
  }

  /** 返回是否开启 */
  public int getOpen() {
    return open;
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
