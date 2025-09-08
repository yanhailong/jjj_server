package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName PrivilegeCard.xlsx
 * @sheetName PrivilegeCard
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PrivilegeCardCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "PrivilegeCard.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "PrivilegeCard";

  /** 每天返利(金币) */
  protected Map<Integer,Long> dayRebate;
  /** 持续天数 */
  protected int days;
  /** 额外道具 */
  protected Map<Integer,Long> getItem;
  /** 活动名称 */
  protected int name;
  /** 购买花费(充值) */
  protected int purchasecost;
  /** 总计返利(金币) */
  protected Map<Integer,Long> totalRebate;
  /** 类型ID */
  protected int type;

  /** 返回每天返利(金币) */
  public Map<Integer,Long> getDayRebate() {
    return dayRebate;
  }

  /** 返回持续天数 */
  public int getDays() {
    return days;
  }

  /** 返回额外道具 */
  public Map<Integer,Long> getGetItem() {
    return getItem;
  }

  /** 返回活动名称 */
  public int getName() {
    return name;
  }

  /** 返回购买花费(充值) */
  public int getPurchasecost() {
    return purchasecost;
  }

  /** 返回总计返利(金币) */
  public Map<Integer,Long> getTotalRebate() {
    return totalRebate;
  }

  /** 返回类型ID */
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
