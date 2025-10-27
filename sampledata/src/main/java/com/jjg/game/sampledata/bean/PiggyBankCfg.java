package com.jjg.game.sampledata.bean;

import java.util.*;
import java.math.BigDecimal;



import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName PiggyBank.xlsx
 * @sheetName PiggyBank
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PiggyBankCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "PiggyBank.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "PiggyBank";

  /** 初始金币 */
  protected int baseGold;
  /** 渠道商品ID */
  protected Map<Integer,String> channelCommodity;
  /** 罐子存储上限 */
  protected long fullUp;
  /** 奖励1 */
  protected Map<Integer,Long> getItem;
  /** 购买金额 */
  protected BigDecimal pay;
  /** 重置时间(天) */
  protected int reseTime;
  /** 罐子类型 */
  protected int type;
  /** 万分比 */
  protected int weight;

  /** 返回初始金币 */
  public int getBaseGold() {
    return baseGold;
  }

  /** 返回渠道商品ID */
  public Map<Integer,String> getChannelCommodity() {
    return channelCommodity;
  }

  /** 返回罐子存储上限 */
  public long getFullUp() {
    return fullUp;
  }

  /** 返回奖励1 */
  public Map<Integer,Long> getGetItem() {
    return getItem;
  }

  /** 返回购买金额 */
  public BigDecimal getPay() {
    return pay;
  }

  /** 返回重置时间(天) */
  public int getReseTime() {
    return reseTime;
  }

  /** 返回罐子类型 */
  public int getType() {
    return type;
  }

  /** 返回万分比 */
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
