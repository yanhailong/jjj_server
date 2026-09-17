package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName ActivePass.xlsx
 * @sheetName ActivePass
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ActivePassCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "ActivePass.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "ActivePass";

  /** 通行证名称 */
  protected int PassName;
  /** 奖励组 */
  protected int RewardGroup;
  /** 高级付费金额 */
  protected int ShopRechargeList1ID;
  /** 初级付费金额 */
  protected int ShopRechargeListID;
  /** 是否开启 */
  protected boolean isOpen;
  /** 结束时间 */
  protected String time_end;
  /** 开启时间 */
  protected String time_start;

  /** 返回通行证名称 */
  public int getPassName() {
    return PassName;
  }

  /** 返回奖励组 */
  public int getRewardGroup() {
    return RewardGroup;
  }

  /** 返回高级付费金额 */
  public int getShopRechargeList1ID() {
    return ShopRechargeList1ID;
  }

  /** 返回初级付费金额 */
  public int getShopRechargeListID() {
    return ShopRechargeListID;
  }

  /** 返回是否开启 */
  public boolean getIsOpen() {
    return isOpen;
  }

  /** 返回结束时间 */
  public String getTime_end() {
    return time_end;
  }

  /** 返回开启时间 */
  public String getTime_start() {
    return time_start;
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
