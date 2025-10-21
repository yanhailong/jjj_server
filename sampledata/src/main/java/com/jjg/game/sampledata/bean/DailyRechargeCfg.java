package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName DailyRecharge.xlsx
 * @sheetName DailyRecharge
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DailyRechargeCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "DailyRecharge.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "DailyRecharge";

  /** 道具数量 */
  protected Map<Integer,Integer> awardItem;
  /** 购买费用 */
  protected int cost;
  /** 购买次数 */
  protected int count;
  /** 礼包名称 */
  protected int name;
  /** 购买类型 */
  protected int payType;
  /** 类型 */
  protected int type;

  /** 返回道具数量 */
  public Map<Integer,Integer> getAwardItem() {
    return awardItem;
  }

  /** 返回购买费用 */
  public int getCost() {
    return cost;
  }

  /** 返回购买次数 */
  public int getCount() {
    return count;
  }

  /** 返回礼包名称 */
  public int getName() {
    return name;
  }

  /** 返回购买类型 */
  public int getPayType() {
    return payType;
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
