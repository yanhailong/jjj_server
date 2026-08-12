package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName SeasonShop.xlsx
 * @sheetName SeasonShop
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SeasonShopCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "SeasonShop.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "SeasonShop";

  /** 限购次数 */
  protected int DailyPurchaseLimit;
  /** 商品 */
  protected Map<Integer,Long> Goods;
  /** 是否开启 */
  protected boolean IsEnabled;
  /** 序列 */
  protected int Order;
  /** 是否每日重置 */
  protected boolean ResetDaily;
  /** 购买价格 */
  protected Map<Integer,Long> cost;
  /** 客户端资源 */
  protected String icon;
  /** 多语言ID */
  protected int language;
  /** 商店类型 */
  protected int type;

  /** 返回限购次数 */
  public int getDailyPurchaseLimit() {
    return DailyPurchaseLimit;
  }

  /** 返回商品 */
  public Map<Integer,Long> getGoods() {
    return Goods;
  }

  /** 返回是否开启 */
  public boolean getIsEnabled() {
    return IsEnabled;
  }

  /** 返回序列 */
  public int getOrder() {
    return Order;
  }

  /** 返回是否每日重置 */
  public boolean getResetDaily() {
    return ResetDaily;
  }

  /** 返回购买价格 */
  public Map<Integer,Long> getCost() {
    return cost;
  }

  /** 返回客户端资源 */
  public String getIcon() {
    return icon;
  }

  /** 返回多语言ID */
  public int getLanguage() {
    return language;
  }

  /** 返回商店类型 */
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
