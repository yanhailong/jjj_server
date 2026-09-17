package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName VisitorQualityAcquisition.xlsx
 * @sheetName VisitorQualityAcquisition
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class VisitorQualityAcquisitionCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "VisitorQualityAcquisition.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "VisitorQualityAcquisition";

  /** 价格类型 */
  protected int CostType;
  /** 单日限购次数 */
  protected int DailyLimitCount;
  /** 价格数量1 */
  protected int PriceValue1;
  /** 单次购买出现数量 */
  protected int QuantityPerPurchase;
  /** 奖励显示 */
  protected Map<Integer,Long> RewardDisplay;
  /** 游客道具ID与权重 */
  protected Map<Integer,Long> VisitorWeight;
  /** 客户端资源 */
  protected String icon;

  /** 返回价格类型 */
  public int getCostType() {
    return CostType;
  }

  /** 返回单日限购次数 */
  public int getDailyLimitCount() {
    return DailyLimitCount;
  }

  /** 返回价格数量1 */
  public int getPriceValue1() {
    return PriceValue1;
  }

  /** 返回单次购买出现数量 */
  public int getQuantityPerPurchase() {
    return QuantityPerPurchase;
  }

  /** 返回奖励显示 */
  public Map<Integer,Long> getRewardDisplay() {
    return RewardDisplay;
  }

  /** 返回游客道具ID与权重 */
  public Map<Integer,Long> getVisitorWeight() {
    return VisitorWeight;
  }

  /** 返回客户端资源 */
  public String getIcon() {
    return icon;
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
