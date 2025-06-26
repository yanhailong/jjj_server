package com.jjg.game.dollarexpress.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName dollarExpressIcon.xlsx
 * @sheetName DollarExpressIcon
 * @author Auto.Generator
 * @date 2025年06月24日 16:33:11
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DollarExpressIconCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "dollarExpressIcon.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "DollarExpressIcon";

  /** 乘以额外倍数 */
  protected int doubling;
  /** 资源名称 */
  protected String icon;
  /** 不中奖时用于填充的权重 */
  protected int noWinning;
  /** 倍率 */
  protected List<Integer> payout;
  /** 图标类型 */
  protected int type;

  /** 返回乘以额外倍数 */
  public int getDoubling() {
    return doubling;
  }

  /** 返回资源名称 */
  public String getIcon() {
    return icon;
  }

  /** 返回不中奖时用于填充的权重 */
  public int getNoWinning() {
    return noWinning;
  }

  /** 返回倍率 */
  public List<Integer> getPayout() {
    return payout;
  }

  /** 返回图标类型 */
  public int getType() {
    return type;
  }
}
