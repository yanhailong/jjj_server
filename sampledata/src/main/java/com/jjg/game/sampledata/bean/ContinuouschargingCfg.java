package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName Continuouscharging.xlsx
 * @sheetName Continuouscharging
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ContinuouschargingCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "Continuouscharging.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "Continuouscharging";

  /** 返利比例（万分比） */
  protected int Rebate;
  /** 任务描述 */
  protected int task;
  /** 类型 */
  protected int type;

  /** 返回返利比例（万分比） */
  public int getRebate() {
    return Rebate;
  }

  /** 返回任务描述 */
  public int getTask() {
    return task;
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
