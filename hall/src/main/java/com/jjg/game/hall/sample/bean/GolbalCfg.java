package com.jjg.game.hall.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName golbal.xlsx
 * @sheetName Golbal
 * @author Auto.Generator
 * @date 2025年06月30日 14:12:12
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class GolbalCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "golbal.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "Golbal";

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
