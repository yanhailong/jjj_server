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

  /** 赌场ID */
  protected int CasinoID;
  /** 序列ID */
  protected int SequenceID;
  /** 解锁消耗 */
  protected Map<Integer,Long> UnlockCost;
  /** 解锁方式 */
  protected int UnlockMethod;
  /** 建筑分类 */
  protected int type;

  /** 返回赌场ID */
  public int getCasinoID() {
    return CasinoID;
  }

  /** 返回序列ID */
  public int getSequenceID() {
    return SequenceID;
  }

  /** 返回解锁消耗 */
  public Map<Integer,Long> getUnlockCost() {
    return UnlockCost;
  }

  /** 返回解锁方式 */
  public int getUnlockMethod() {
    return UnlockMethod;
  }

  /** 返回建筑分类 */
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
