package com.jjg.game.sampledata.bean;

import java.util.*;
import java.math.BigDecimal;



import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName FirstPayment.xlsx
 * @sheetName firstpayment
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class FirstpaymentCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "FirstPayment.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "firstpayment";

  /** 超值 */
  protected BigDecimal bestValue;
  /** 渠道商品ID */
  protected Map<Integer,String> channelCommodity;
  /** 头像框奖励 */
  protected Map<Integer,Long> getAvatarFrame;
  /** 金币奖励 */
  protected Map<Integer,Long> getgold;
  /** 道具奖励 */
  protected Map<Integer,Long> getitem;
  /** 购买金额 */
  protected BigDecimal money;
  /** 原价值 */
  protected BigDecimal wasPrice;

  /** 返回超值 */
  public BigDecimal getBestValue() {
    return bestValue;
  }

  /** 返回渠道商品ID */
  public Map<Integer,String> getChannelCommodity() {
    return channelCommodity;
  }

  /** 返回头像框奖励 */
  public Map<Integer,Long> getGetAvatarFrame() {
    return getAvatarFrame;
  }

  /** 返回金币奖励 */
  public Map<Integer,Long> getGetgold() {
    return getgold;
  }

  /** 返回道具奖励 */
  public Map<Integer,Long> getGetitem() {
    return getitem;
  }

  /** 返回购买金额 */
  public BigDecimal getMoney() {
    return money;
  }

  /** 返回原价值 */
  public BigDecimal getWasPrice() {
    return wasPrice;
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
