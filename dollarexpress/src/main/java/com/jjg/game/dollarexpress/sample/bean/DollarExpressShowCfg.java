package com.jjg.game.dollarexpress.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName dollarExpressShow.xlsx
 * @sheetName DollarExpressShow
 * @author Auto.Generator
 * @date 2025年06月24日 16:33:11
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DollarExpressShowCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "dollarExpressShow.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "DollarExpressShow";

  /** 图标集合 */
  protected List<Integer> icons;

  /** 返回图标集合 */
  public List<Integer> getIcons() {
    return icons;
  }
}
