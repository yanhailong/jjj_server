package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName BuildingUpgradeTable.xlsx
 * @sheetName BuildingUpgradeTable
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BuildingUpgradeTableCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "BuildingUpgradeTable.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "BuildingUpgradeTable";

  /** 建筑ID */
  protected int BuildingID;
  /** 每级消耗 */
  protected Map<Integer,Long> CostPerLevel;
  /** 最大交互数量 */
  protected int MaxInteractionCount;
  /** 解锁设备 */
  protected List<Integer> UnlockEquipment;
  /** 建筑升级CD时间（min） */
  protected int UpgradeCD;
  /** 升级消耗道具 */
  protected Map<Integer,Long> UpgradeCost;
  /** 建筑基础属性 */
  protected Map<Integer,Long> UpgradeOutput;
  /** 等级 */
  protected int level;
  /** 多语言 */
  protected int text;

  /** 返回建筑ID */
  public int getBuildingID() {
    return BuildingID;
  }

  /** 返回每级消耗 */
  public Map<Integer,Long> getCostPerLevel() {
    return CostPerLevel;
  }

  /** 返回最大交互数量 */
  public int getMaxInteractionCount() {
    return MaxInteractionCount;
  }

  /** 返回解锁设备 */
  public List<Integer> getUnlockEquipment() {
    return UnlockEquipment;
  }

  /** 返回建筑升级CD时间（min） */
  public int getUpgradeCD() {
    return UpgradeCD;
  }

  /** 返回升级消耗道具 */
  public Map<Integer,Long> getUpgradeCost() {
    return UpgradeCost;
  }

  /** 返回建筑基础属性 */
  public Map<Integer,Long> getUpgradeOutput() {
    return UpgradeOutput;
  }

  /** 返回等级 */
  public int getLevel() {
    return level;
  }

  /** 返回多语言 */
  public int getText() {
    return text;
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
