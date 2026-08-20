package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName VisitorTargetList.xlsx
 * @sheetName VisitorTargetList
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class VisitorTargetListCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "VisitorTargetList.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "VisitorTargetList";

  /** 是否每日刷新 */
  protected boolean DailyRefresh;
  /** 每日刷新的次数上限 */
  protected int DailyRefreshLimit;
  /** 单日观看次数上限 */
  protected int DailyViewLimit;
  /** 界面显示的个数 */
  protected int DisplayCount;
  /** 是否可以手动刷新 */
  protected boolean ManualRefresh;
  /** 卡池类型 */
  protected int PoolType;
  /** 手动刷新的费用 */
  protected List<List<Integer>> RefreshCost;
  /** 场景ID */
  protected int RegionID;
  /** 礼包出现游客的种类个数 */
  protected int visitorGiftPackCount;

  /** 返回是否每日刷新 */
  public boolean getDailyRefresh() {
    return DailyRefresh;
  }

  /** 返回每日刷新的次数上限 */
  public int getDailyRefreshLimit() {
    return DailyRefreshLimit;
  }

  /** 返回单日观看次数上限 */
  public int getDailyViewLimit() {
    return DailyViewLimit;
  }

  /** 返回界面显示的个数 */
  public int getDisplayCount() {
    return DisplayCount;
  }

  /** 返回是否可以手动刷新 */
  public boolean getManualRefresh() {
    return ManualRefresh;
  }

  /** 返回卡池类型 */
  public int getPoolType() {
    return PoolType;
  }

  /** 返回手动刷新的费用 */
  public List<List<Integer>> getRefreshCost() {
    return RefreshCost;
  }

  /** 返回场景ID */
  public int getRegionID() {
    return RegionID;
  }

  /** 返回礼包出现游客的种类个数 */
  public int getVisitorGiftPackCount() {
    return visitorGiftPackCount;
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
