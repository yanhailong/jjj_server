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
  protected Map<Integer,Long> Buff;
  /** 序列 */
  protected int Index;
  /** 加成（百分比） */
  protected Map<Integer,Long> Modifier;
  /** 品质 */
  protected int Quality;
  /** 技能名称多语言 */
  protected int SkillName;
  /** 类型 */
  protected int type;

  /** 返回固定值加成 */
  public Map<Integer,Long> getBuff() {
    return Buff;
  }

  /** 返回序列 */
  public int getIndex() {
    return Index;
  }

  /** 返回加成（百分比） */
  public Map<Integer,Long> getModifier() {
    return Modifier;
  }

  /** 返回品质 */
  public int getQuality() {
    return Quality;
  }

  /** 返回技能名称多语言 */
  public int getSkillName() {
    return SkillName;
  }

  /** 返回类型 */
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
