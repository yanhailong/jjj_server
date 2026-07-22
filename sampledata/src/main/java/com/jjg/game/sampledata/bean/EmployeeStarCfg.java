package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName EmployeeStar.xlsx
 * @sheetName EmployeeStar
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class EmployeeStarCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "EmployeeStar.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "EmployeeStar";

  /** 雇员ID */
  protected int EmployeeID;
  /** 等级上限 */
  protected int LevelCap;
  /** 星级 */
  protected int Star;
  /** 升星碎片消耗 */
  protected int StarUpCost;

  /** 返回雇员ID */
  public int getEmployeeID() {
    return EmployeeID;
  }

  /** 返回等级上限 */
  public int getLevelCap() {
    return LevelCap;
  }

  /** 返回星级 */
  public int getStar() {
    return Star;
  }

  /** 返回升星碎片消耗 */
  public int getStarUpCost() {
    return StarUpCost;
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
