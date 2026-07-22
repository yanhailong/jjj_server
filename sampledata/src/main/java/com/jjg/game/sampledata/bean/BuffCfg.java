package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName Buff.xlsx
 * @sheetName Buff
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BuffCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "Buff.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "Buff";

  /** 条件参数数量 */
  protected int BuffParamNum;

  /** 返回条件参数数量 */
  public int getBuffParamNum() {
    return BuffParamNum;
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
