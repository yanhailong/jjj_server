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
  protected List<List<Integer>> CostPerLevel;
  /** 最大交互数量 */
  protected int MaxInteractionCount;
  /** 经营等级 */
  protected int NeedLevel;
  /** 解锁装饰 */
  protected List<Integer> UnlockDecorationId;
  /** 解锁设备 */
  protected List<Integer> UnlockEquipment;
  /** 交互点ID */
  protected List<Integer> UnlockInteractionId;
  /** 建筑升级CD时间（min） */
  protected int UpgradeCD;
  /** 升级消耗道具 */
  protected Map<Integer,Long> UpgradeCost;
  /** 建筑基础属性 */
  protected long UpgradeExp;
  /** 建筑基础属性 */
  protected long UpgradeOutput;
  /** 升级奖励 */
  protected Map<Integer,Long> UpgradeReward;
  /** 等级 */
  protected int level;
  /** 多语言 */
  protected int text;

  /** 返回建筑ID */
  public int getBuildingID() {
    return BuildingID;
  }

  /** 返回每级消耗 */
  public List<List<Integer>> getCostPerLevel() {
    return CostPerLevel;
  }

  /** 返回最大交互数量 */
  public int getMaxInteractionCount() {
    return MaxInteractionCount;
  }

  /** 返回经营等级 */
  public int getNeedLevel() {
    return NeedLevel;
  }

  /** 返回解锁装饰 */
  public List<Integer> getUnlockDecorationId() {
    return UnlockDecorationId;
  }

  /** 返回解锁设备 */
  public List<Integer> getUnlockEquipment() {
    return UnlockEquipment;
  }

  /** 返回交互点ID */
  public List<Integer> getUnlockInteractionId() {
    return UnlockInteractionId;
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
  public long getUpgradeExp() {
    return UpgradeExp;
  }

  /** 返回建筑基础属性 */
  public long getUpgradeOutput() {
    return UpgradeOutput;
  }

  /** 返回升级奖励 */
  public Map<Integer,Long> getUpgradeReward() {
    return UpgradeReward;
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
