package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName BundleGiftPack.xlsx
 * @sheetName BundleGiftPack
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BundleGiftPackCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "BundleGiftPack.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "BundleGiftPack";

  /** 渠道商品ID */
  protected int ChannelCommodity;
  /** 礼包内容 */
  protected Map<Integer,Long> GetItem;

  /** 返回渠道商品ID */
  public int getChannelCommodity() {
    return ChannelCommodity;
  }

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
