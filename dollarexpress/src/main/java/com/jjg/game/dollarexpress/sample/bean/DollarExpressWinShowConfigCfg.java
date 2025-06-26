package com.jjg.game.dollarexpress.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName dollarExpressWinShow.xlsx
 * @sheetName DollarExpressWinShowConfig
 * @author Auto.Generator
 * @date 2025年06月24日 16:33:11
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DollarExpressWinShowConfigCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "dollarExpressWinShow.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "DollarExpressWinShowConfig";

  /** 特效资源 */
  protected String icon;
  /** 倍率=奖金/底注 */
  protected int payout_min;

  /** 返回特效资源 */
  public String getIcon() {
    return icon;
  }

  /** 返回倍率=奖金/底注 */
  public int getPayout_min() {
    return payout_min;
  }
}
