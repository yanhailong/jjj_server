package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName SkillConfig.xlsx
 * @sheetName SkillConfig
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SkillConfigCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "SkillConfig.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "SkillConfig";

  /** 固定值加成 */
  protected Map<Integer,Long> Buff;
  /** 加成（百分比） */
  protected Map<Integer,Long> Modifier;
  /** 技能 */
  protected int Skill;

  /** 返回固定值加成 */
  public Map<Integer,Long> getBuff() {
    return Buff;
  }

  /** 返回加成（百分比） */
  public Map<Integer,Long> getModifier() {
    return Modifier;
  }

  /** 返回技能 */
  public int getSkill() {
    return Skill;
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
