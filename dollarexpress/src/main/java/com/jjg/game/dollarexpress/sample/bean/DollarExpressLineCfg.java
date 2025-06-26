package com.jjg.game.dollarexpress.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName dollarExpressLine.xlsx
 * @sheetName DollarExpressLine
 * @author Auto.Generator
 * @date 2025年06月24日 16:33:11
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DollarExpressLineCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "dollarExpressLine.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "DollarExpressLine";

  /** 中奖线轴 */
  protected List<Integer> yLine;

  /** 返回中奖线轴 */
  public List<Integer> getYLine() {
    return yLine;
  }
}
