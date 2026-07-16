package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName MiningBundleShop.xlsx
 * @sheetName MiningBundleShop
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class MiningBundleShopCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "MiningBundleShop.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "MiningBundleShop";

  /** 礼包名称 */
  protected int BundleName;
  /** 每日限购次数 */
  protected int DailyPurchaseLimit;
  /** 商品 */
  protected List<Integer> Goods;
  /** 序列 */
  protected int Order;
  /** 购买价格 */
  protected List<Integer> cost;
  /** 客户端资源 */
  protected String icon;
  /** 多语言ID */
  protected int language;
  /** 商店类型 */
  protected int type;

  /** 返回礼包名称 */
  public int getBundleName() {
    return BundleName;
  }

  /** 返回每日限购次数 */
  public int getDailyPurchaseLimit() {
    return DailyPurchaseLimit;
  }

  /** 返回商品 */
  public List<Integer> getGoods() {
    return Goods;
  }

  /** 返回序列 */
  public int getOrder() {
    return Order;
  }

  /** 返回购买价格 */
  public List<Integer> getCost() {
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
