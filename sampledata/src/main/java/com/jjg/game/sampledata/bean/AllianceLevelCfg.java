package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName AllianceLevel.xlsx
 * @sheetName AllianceLevel
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class AllianceLevelCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "AllianceLevel.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "AllianceLevel";

  /** 成员上限 */
  protected int MaxMembers;
  /** 升级所需声誉值 */
  protected int ReputationRequired;
  /** 等级 */
  protected int level;

  /** 返回成员上限 */
  public int getMaxMembers() {
    return MaxMembers;
  }

  /** 返回升级所需声誉值 */
  public int getReputationRequired() {
    return ReputationRequired;
  }

  /** 返回等级 */
  public int getLevel() {
    return level;
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
