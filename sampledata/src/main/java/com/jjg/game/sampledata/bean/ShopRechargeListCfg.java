package com.jjg.game.sampledata.bean;

import java.util.*;
import java.math.BigDecimal;



import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName ShopRechargeList.xlsx
 * @sheetName ShopRechargeList
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ShopRechargeListCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "ShopRechargeList.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "ShopRechargeList";

  /** 金额 */
  protected BigDecimal Price;
  /** google渠道商城ID */
  protected String googleShopId;
  /** ios渠道商城ID */
  protected String iosShopId;

  /** 返回金额 */
  public BigDecimal getPrice() {
    return Price;
  }

  /** 返回google渠道商城ID */
  public String getGoogleShopId() {
    return googleShopId;
  }

  /** 返回ios渠道商城ID */
  public String getIosShopId() {
    return iosShopId;
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
