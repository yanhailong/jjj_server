package com.jjg.game.slots.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName golbal.xlsx
 * @sheetName GolbalConfig
 * @author Auto.Generator
 * @date 2025年08月02日 14:18:48
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class GolbalConfigCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "golbal.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "GolbalConfig";

  /** 备注 */
  protected String mark;
  /** 名称 */
  protected String name;
  /** 值 */
  protected String value;

  /** 返回备注 */
  public String getMark() {
    return mark;
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
