package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName EmployeeSkillConfig.xlsx
 * @sheetName EmployeeSkillConfig
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class EmployeeSkillConfigCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "EmployeeSkillConfig.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "EmployeeSkillConfig";

  /** 固定值加成 */
  protected Map<Integer,Integer> Buff;
  /** 加成（百分比） */
  protected Map<Integer,Integer> Modifier;
  /** 技能名称多语言 */
  protected int SkillName;

  /** 返回固定值加成 */
  public Map<Integer,Integer> getBuff() {
    return Buff;
  }

  /** 返回加成（百分比） */
  public Map<Integer,Integer> getModifier() {
    return Modifier;
  }

  /** 返回技能名称多语言 */
  public int getSkillName() {
    return SkillName;
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
