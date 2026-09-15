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

  /** 每日的刷新时间点 */
  protected List<Integer> DailyRefreshTime;
  /** 单日观看次数上限 */
  protected int DailyViewLimit;
  /** 界面显示的个数 */
  protected int DisplayCount;
  /** 是否开启 */
  protected boolean IsEnabled;
  /** 是否按时段刷新 */
  protected boolean IsRefreshByTimePeriod;
  /** 是否可以手动刷新 */
  protected boolean ManualRefresh;
  /** 每个时间段手动刷新的次数上限 */
  protected int MaxManualRefreshPerPeriod;
  /** 每次刷新出现的游客购买次数上限 */
  protected int MaxPurchasePerRefresh;
  /** 卡池类型 */
  protected int PoolType;
  /** 礼包出现的概率(百分比） */
  protected int Rate;
  /** 手动刷新的费用 */
  protected List<List<Integer>> RefreshCost;
  /** 场景ID */
  protected int RegionID;
  /** 出现的游客_权重 */
  protected List<List<Integer>> VisitorWeight;
  /** 礼包出现游客的种类个数 */
  protected int visitorGiftPackCount;

  /** 返回每日的刷新时间点 */
  public List<Integer> getDailyRefreshTime() {
    return DailyRefreshTime;
  }

  /** 返回单日观看次数上限 */
  public int getDailyViewLimit() {
    return DailyViewLimit;
  }

  /** 返回界面显示的个数 */
  public int getDisplayCount() {
    return DisplayCount;
  }

  /** 返回是否开启 */
  public boolean getIsEnabled() {
    return IsEnabled;
  }

  /** 返回是否按时段刷新 */
  public boolean getIsRefreshByTimePeriod() {
    return IsRefreshByTimePeriod;
  }

  /** 返回是否可以手动刷新 */
  public boolean getManualRefresh() {
    return ManualRefresh;
  }

  /** 返回每个时间段手动刷新的次数上限 */
  public int getMaxManualRefreshPerPeriod() {
    return MaxManualRefreshPerPeriod;
  }

  /** 返回每次刷新出现的游客购买次数上限 */
  public int getMaxPurchasePerRefresh() {
    return MaxPurchasePerRefresh;
  }

  /** 返回卡池类型 */
  public int getPoolType() {
    return PoolType;
  }

  /** 返回礼包出现的概率(百分比） */
  public int getRate() {
    return Rate;
  }

  /** 返回手动刷新的费用 */
  public List<List<Integer>> getRefreshCost() {
    return RefreshCost;
  }

  /** 返回场景ID */
  public int getRegionID() {
    return RegionID;
  }

  /** 返回出现的游客_权重 */
  public List<List<Integer>> getVisitorWeight() {
    return VisitorWeight;
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
