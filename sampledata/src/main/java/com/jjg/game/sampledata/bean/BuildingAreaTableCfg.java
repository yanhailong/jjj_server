package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName BuildingAreaTable.xlsx
 * @sheetName BuildingAreaTable
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BuildingAreaTableCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "BuildingAreaTable.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "BuildingAreaTable";

  /** 建筑名称ID */
  protected int BuildingNameId;
  /** 解锁条件1 */
  protected int CasinoLevel;
  /** 受影响的雇员类型 */
  protected int EmployeeProfile;
  /** 交互时间(S) */
  protected int InteractTime;
  /** 等级上限 */
  protected int MaxLevel;
  /** 场景ID */
  protected int RegionID;
  /** 解锁消耗 */
  protected Map<Integer,Long> UnlockCost;
  /** 解锁游戏ID */
  protected int UnlockGameId;
  /** 解锁条件2 */
  protected Map<Integer,Integer> UnlockMethod;
  /** 解锁方式 */
  protected boolean UnlockType;
  /** 建筑分类 */
  protected int type;
  /** 建筑属性分类 */
  protected List<Integer> typeValue;

  /** 返回建筑名称ID */
  public int getBuildingNameId() {
    return BuildingNameId;
  }

  /** 返回解锁条件1 */
  public int getCasinoLevel() {
    return CasinoLevel;
  }

  /** 返回受影响的雇员类型 */
  public int getEmployeeProfile() {
    return EmployeeProfile;
  }

  /** 返回交互时间(S) */
  public int getInteractTime() {
    return InteractTime;
  }

  /** 返回等级上限 */
  public int getMaxLevel() {
    return MaxLevel;
  }

  /** 返回场景ID */
  public int getRegionID() {
    return RegionID;
  }

  /** 返回解锁消耗 */
  public Map<Integer,Long> getUnlockCost() {
    return UnlockCost;
  }

  /** 返回解锁游戏ID */
  public int getUnlockGameId() {
    return UnlockGameId;
  }

  /** 返回解锁条件2 */
  public Map<Integer,Integer> getUnlockMethod() {
    return UnlockMethod;
  }

  /** 返回解锁方式 */
  public boolean getUnlockType() {
    return UnlockType;
  }

  /** 返回建筑分类 */
  public int getType() {
    return type;
  }

  /** 返回建筑属性分类 */
  public List<Integer> getTypeValue() {
    return typeValue;
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
