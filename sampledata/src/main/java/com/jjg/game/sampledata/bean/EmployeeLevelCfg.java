package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName EmployeeLevel.xlsx
 * @sheetName EmployeeLevel
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class EmployeeLevelCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "EmployeeLevel.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "EmployeeLevel";

  /** 雇员ID */
  protected int EmployeeID;
  /** 等级 */
  protected int Level;
  /** 技能值 */
  protected Map<Integer,Integer> SkillValue;
  /** 升级消耗 */
  protected Map<Integer,Integer> UpgradeCost;

  /** 返回雇员ID */
  public int getEmployeeID() {
    return EmployeeID;
  }

  /** 返回等级 */
  public int getLevel() {
    return Level;
  }

  /** 返回技能值 */
  public Map<Integer,Integer> getSkillValue() {
    return SkillValue;
  }

  /** 返回升级消耗 */
  public Map<Integer,Integer> getUpgradeCost() {
    return UpgradeCost;
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
