package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName VisitorBonds.xlsx
 * @sheetName VisitorBonds
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class VisitorBondsCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "VisitorBonds.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "VisitorBonds";

  /** 成员组成 */
  protected List<Integer> Members;
  /** 多语言 */
  protected int NameId;
  /** 羁绊奖励 */
  protected Map<Integer,Long> Reward;

  /** 返回成员组成 */
  public List<Integer> getMembers() {
    return Members;
  }

  /** 返回多语言 */
  public int getNameId() {
    return NameId;
  }

  /** 返回羁绊奖励 */
  public Map<Integer,Long> getReward() {
    return Reward;
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
