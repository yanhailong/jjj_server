package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName CasinoStatsSheet.xlsx
 * @sheetName CasinoStatsSheet
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class CasinoStatsSheetCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "CasinoStatsSheet.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "CasinoStatsSheet";

  /** 游客基础来访间隔(秒) */
  protected int BaseVisitInterval;
  /** 消耗效率（百分比） */
  protected int DrainRate;
  /** 预估建筑达成等级 */
  protected Map<Integer,Integer> ExpectedBuildingLevel;
  /** 曝光要求 */
  protected int ExposureRequirements;
  /** 升级前置条件 */
  protected Map<Integer,Integer> LevelUpCondition;
  /** 延长游客生成间隔的倍数上限 */
  protected int MaxMultiplier;
  /** 离线收益时长（分） */
  protected int OfflineDuration;
  /** 基础繁荣度 */
  protected int Prosperity;
  /** 场景 ID */
  protected int RegionID;
  /** 升级消耗数量 */
  protected int UpgradeCost;
  /** 升级产出能量 */
  protected int UpgradeOutput;
  /** 游客每日交互时长上限（min） */
  protected int VisitorDailyLimit;
  /** 游客生成数量 */
  protected int VisitorSpawnCount;
  /** 等级 */
  protected int level;

  /** 返回游客基础来访间隔(秒) */
  public int getBaseVisitInterval() {
    return BaseVisitInterval;
  }

  /** 返回消耗效率（百分比） */
  public int getDrainRate() {
    return DrainRate;
  }

  /** 返回预估建筑达成等级 */
  public Map<Integer,Integer> getExpectedBuildingLevel() {
    return ExpectedBuildingLevel;
  }

  /** 返回曝光要求 */
  public int getExposureRequirements() {
    return ExposureRequirements;
  }

  /** 返回升级前置条件 */
  public Map<Integer,Integer> getLevelUpCondition() {
    return LevelUpCondition;
  }

  /** 返回延长游客生成间隔的倍数上限 */
  public int getMaxMultiplier() {
    return MaxMultiplier;
  }

  /** 返回离线收益时长（分） */
  public int getOfflineDuration() {
    return OfflineDuration;
  }

  /** 返回基础繁荣度 */
  public int getProsperity() {
    return Prosperity;
  }

  /** 返回场景 ID */
  public int getRegionID() {
    return RegionID;
  }

  /** 返回升级消耗数量 */
  public int getUpgradeCost() {
    return UpgradeCost;
  }

  /** 返回升级产出能量 */
  public int getUpgradeOutput() {
    return UpgradeOutput;
  }

  /** 返回游客每日交互时长上限（min） */
  public int getVisitorDailyLimit() {
    return VisitorDailyLimit;
  }

  /** 返回游客生成数量 */
  public int getVisitorSpawnCount() {
    return VisitorSpawnCount;
  }

  /** 返回等级 */
  public int getLevel() {
    return level;
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
