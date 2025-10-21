package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName global.xlsx
 * @sheetName globalConfig
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class GlobalConfigCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "global.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "globalConfig";

  /** 布尔值 */
  protected boolean boolValue;
  /** 整形 */
  protected int intValue;
  /** 长整型 */
  protected long longValue;
  /** 值 */
  protected String value;

  /** 返回布尔值 */
  public boolean getBoolValue() {
    return boolValue;
  }

  /** 返回整形 */
  public int getIntValue() {
    return intValue;
  }

  /** 返回长整型 */
  public long getLongValue() {
    return longValue;
  }

  /** 返回值 */
  public String getValue() {
    return value;
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
