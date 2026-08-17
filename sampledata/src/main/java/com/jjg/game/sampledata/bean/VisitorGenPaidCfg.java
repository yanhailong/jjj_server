package com.jjg.game.sampledata.bean;

import java.util.*;
import java.math.BigDecimal;



import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName VisitorGenPaid.xlsx
 * @sheetName VisitorGenPaid
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class VisitorGenPaidCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "VisitorGenPaid.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "VisitorGenPaid";

  /** 价格类型 */
  protected int CostType;
  /** 单日限购次数 */
  protected int DailyLimitCount;
  /** 价格数量1 */
  protected BigDecimal PriceValue1;
  /** 单个游客数量 */
  protected int VisitorCount;
  /** 游客道具ID */
  protected int VisitorID;
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
  public BigDecimal getPriceValue1() {
    return PriceValue1;
  }

  /** 返回单个游客数量 */
  public int getVisitorCount() {
    return VisitorCount;
  }

  /** 返回游客道具ID */
  public int getVisitorID() {
    return VisitorID;
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
