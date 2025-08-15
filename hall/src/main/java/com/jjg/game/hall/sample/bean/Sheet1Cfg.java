package com.jjg.game.hall.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName condition.xlsx
 * @sheetName Sheet1
 * @author Auto.Generator
 * @date 2025年08月15日 15:43:07
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class Sheet1Cfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "condition.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "Sheet1";

  /** 条件参数数量 */
  protected int conditionParameter;
  /** 关键参数 */
  protected String conditionType;
  /** 条件不满足时多语言ID */
  protected int languageID;
  /** 关键参数格式 */
  protected String text;

  /** 返回条件参数数量 */
  public int getConditionParameter() {
    return conditionParameter;
  }

  /** 返回关键参数 */
  public String getConditionType() {
    return conditionType;
  }

  /** 返回条件不满足时多语言ID */
  public int getLanguageID() {
    return languageID;
  }

  /** 返回关键参数格式 */
  public String getText() {
    return text;
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
