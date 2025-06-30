package com.jjg.game.slots.game.dollarexpress.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName dollarExpressGolbal.xlsx
 * @sheetName DollarExpressGolbalConfig
 * @author Auto.Generator
 * @date 2025年06月27日 09:57:50
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DollarExpressGolbalConfigCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "dollarExpressGolbal.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "DollarExpressGolbalConfig";

  /** 数字值 */
  protected int intValue;
  /** 名称 */
  protected String name;
  /** 值 */
  protected String value;

  /** 返回数字值 */
  public int getIntValue() {
    return intValue;
  }

  /** 返回名称 */
  public String getName() {
    return name;
  }

  /** 返回值 */
  public String getValue() {
    return value;
  }
}
