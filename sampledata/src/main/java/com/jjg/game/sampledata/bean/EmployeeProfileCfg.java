package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName EmployeeProfile.xlsx
 * @sheetName EmployeeProfile
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class EmployeeProfileCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "EmployeeProfile.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "EmployeeProfile";

  /** 道具ID_碎片ID_碎片数量 */
  protected List<Integer> DuplicatetoShard;
  /** 雇员品质 */
  protected int EmployeeQuality;
  /** 雇员名称 */
  protected int NameId;
  /** 职业ID */
  protected int ProfessionID;
  /** 序列ID */
  protected int SequenceID;
  /** 技能 */
  protected List<Integer> SkillIdList;

  /** 返回道具ID_碎片ID_碎片数量 */
  public List<Integer> getDuplicatetoShard() {
    return DuplicatetoShard;
  }

  /** 返回雇员品质 */
  public int getEmployeeQuality() {
    return EmployeeQuality;
  }

  /** 返回雇员名称 */
  public int getNameId() {
    return NameId;
  }

  /** 返回职业ID */
  public int getProfessionID() {
    return ProfessionID;
  }

  /** 返回序列ID */
  public int getSequenceID() {
    return SequenceID;
  }

  /** 返回技能 */
  public List<Integer> getSkillIdList() {
    return SkillIdList;
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
