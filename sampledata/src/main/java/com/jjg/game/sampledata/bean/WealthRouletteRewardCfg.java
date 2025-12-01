package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName WealthRouletteReward.xlsx
 * @sheetName WealthRouletteReward
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class WealthRouletteRewardCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "WealthRouletteReward.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "WealthRouletteReward";

  /** 金币档位 */
  protected Map<Integer,Long> item;
  /** 客户端图片资源 */
  protected String picture;
  /** 权重（千分比） */
  protected int weight;

  /** 返回金币档位 */
  public Map<Integer,Long> getItem() {
    return item;
  }

  /** 返回客户端图片资源 */
  public String getPicture() {
    return picture;
  }

  /** 返回权重（千分比） */
  public int getWeight() {
    return weight;
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
