package com.jjg.game.sampledata.bean;

import javax.annotation.processing.Generated;
import java.util.Map;
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
  protected int frequency;
  /** 商品奖励 */
  protected Map<Integer,Long> item;
  /** 是否开启 */
  protected int open;
  /** 购买消耗 */
  protected int purchase;
  /** 商店表序列 */
  protected int sequence;

  /** 返回每日限购次数 */
  public int getFrequency() {
    return frequency;
  }

  /** 返回商品奖励 */
  public Map<Integer,Long> getItem() {
    return item;
  }

  /** 返回是否开启 */
  public int getOpen() {
    return open;
  }

  /** 返回购买消耗 */
  public int getPurchase() {
    return purchase;
  }

  /** 返回商店表序列 */
  public int getSequence() {
    return sequence;
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
