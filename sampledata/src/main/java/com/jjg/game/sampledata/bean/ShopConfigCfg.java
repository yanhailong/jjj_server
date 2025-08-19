package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName shop.xlsx
 * @sheetName ShopConfig
 * @author Auto.Generator
 * @date 2025年08月19日 11:16:26
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ShopConfigCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "shop.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "ShopConfig";

  /** 天数规则 */
  protected int day;
  /** 折扣原价 */
  protected String discount;
  /** 折扣 */
  protected String discountratio;
  /** 存在时间 */
  protected String duration;
  /** 图标 */
  protected String icon1;
  /** 标签 */
  protected String icon2;
  /** 道具奖励 */
  protected String item;
  /** 等级条件 */
  protected int level;
  /** 购买价格 */
  protected int money;
  /** 分类 */
  protected int type;
  /** 指定日期开放 */
  protected String yymmdd;

  /** 返回天数规则 */
  public int getDay() {
    return day;
  }

  /** 返回折扣原价 */
  public String getDiscount() {
    return discount;
  }

  /** 返回折扣 */
  public String getDiscountratio() {
    return discountratio;
  }

  /** 返回存在时间 */
  public String getDuration() {
    return duration;
  }

  /** 返回图标 */
  public String getIcon1() {
    return icon1;
  }

  /** 返回标签 */
  public String getIcon2() {
    return icon2;
  }

  /** 返回道具奖励 */
  public String getItem() {
    return item;
  }

  /** 返回等级条件 */
  public int getLevel() {
    return level;
  }

  /** 返回购买价格 */
  public int getMoney() {
    return money;
  }

  /** 返回分类 */
  public int getType() {
    return type;
  }

  /** 返回指定日期开放 */
  public String getYymmdd() {
    return yymmdd;
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
