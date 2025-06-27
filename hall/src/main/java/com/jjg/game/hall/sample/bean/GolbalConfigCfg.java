package com.jjg.game.hall.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName golbal.xlsx
 * @sheetName GolbalConfig
 * @author Auto.Generator
 * @date 2025年06月27日 16:42:37
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class GolbalConfigCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "golbal.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "GolbalConfig";

  /** 备注 */
  protected String mark;
  /** 值 */
  protected int value;

  /** 返回备注 */
  public String getMark() {
    return mark;
  }

  /** 返回值 */
  public int getValue() {
    return value;
  }
}
