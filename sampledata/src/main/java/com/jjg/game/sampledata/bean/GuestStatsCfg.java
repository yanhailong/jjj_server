package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName GuestStats.xlsx
 * @sheetName GuestStats
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class GuestStatsCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "GuestStats.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "GuestStats";

  /** 游客基础来访间隔(秒) */
  protected int BaseVisitInterval;
  /** 基础刷新权重 */
  protected int BaseWeight;
  /** 曝光时的刷新权重 */
  protected int RefreshWeights;
  /** 曝光度来访间隔系数 */
  protected int VisitIntervalCoefficient;
  /** 知名度要求 */
  protected int awareness;
  /** 游客品质 */
  protected int type;
  /** 解锁道具 */
  protected Map<Integer,Integer> unlockitems;

  /** 返回游客基础来访间隔(秒) */
  public int getBaseVisitInterval() {
    return BaseVisitInterval;
  }

  /** 返回基础刷新权重 */
  public int getBaseWeight() {
    return BaseWeight;
  }

  /** 返回曝光时的刷新权重 */
  public int getRefreshWeights() {
    return RefreshWeights;
  }

  /** 返回曝光度来访间隔系数 */
  public int getVisitIntervalCoefficient() {
    return VisitIntervalCoefficient;
  }

  /** 返回知名度要求 */
  public int getAwareness() {
    return awareness;
  }

  /** 返回游客品质 */
  public int getType() {
    return type;
  }

  /** 返回解锁道具 */
  public Map<Integer,Integer> getUnlockitems() {
    return unlockitems;
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
