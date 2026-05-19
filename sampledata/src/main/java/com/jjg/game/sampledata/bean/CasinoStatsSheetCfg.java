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

  /** 开启曝光所需值 */
  protected int ExposureValue;
  /** 基础繁荣度 */
  protected int Prosperity;
  /** 升级消耗道具 */
  protected Map<Integer,Long> UpgradeCost;
  /** 升级产出能量 */
  protected int UpgradeOutput;
  /** 曝光持续基础时长（秒） */
  protected int ViewableDuration;
  /** 等级 */
  protected int level;

  /** 返回开启曝光所需值 */
  public int getExposureValue() {
    return ExposureValue;
  }

  /** 返回基础繁荣度 */
  public int getProsperity() {
    return Prosperity;
  }

  /** 返回升级消耗道具 */
  public Map<Integer,Long> getUpgradeCost() {
    return UpgradeCost;
  }

  /** 返回升级产出能量 */
  public int getUpgradeOutput() {
    return UpgradeOutput;
  }

  /** 返回曝光持续基础时长（秒） */
  public int getViewableDuration() {
    return ViewableDuration;
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
