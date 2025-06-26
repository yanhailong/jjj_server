package com.jjg.game.dollarexpress.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName dollarExpressWeightIcon.xlsx
 * @sheetName DollarExpressWeightIconConfig
 * @author Auto.Generator
 * @date 2025年06月24日 16:33:11
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DollarExpressWeightIconConfigCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "dollarExpressWeightIcon.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "DollarExpressWeightIconConfig";

  /** 美元权重_30 */
  protected int iconID_30;
  /** 火车权重_31 */
  protected int iconID_31;
  /** 1连赔付 */
  protected int payout;

  /** 返回美元权重_30 */
  public int getIconID_30() {
    return iconID_30;
  }

  /** 返回火车权重_31 */
  public int getIconID_31() {
    return iconID_31;
  }

  /** 返回1连赔付 */
  public int getPayout() {
    return payout;
  }
}
