package com.jjg.game.dollarexpress.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName dollarExpressFreeWeight.xlsx
 * @sheetName DollarExpressFreeWeightConfig
 * @author Auto.Generator
 * @date 2025年06月24日 16:33:11
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DollarExpressFreeWeightConfigCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "dollarExpressFreeWeight.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "DollarExpressFreeWeightConfig";

  /** 百搭数量 */
  protected int num;
  /** 1倍百搭_出现权重 */
  protected int wild_1_weight;
  /** 2倍百搭_出现权重 */
  protected int wild_2_weight;
  /** 5倍百搭_出现权重 */
  protected int wild_5_weight;
  /** 1倍百搭_至少数量 */
  protected int wild_min;

  /** 返回百搭数量 */
  public int getNum() {
    return num;
  }

  /** 返回1倍百搭_出现权重 */
  public int getWild_1_weight() {
    return wild_1_weight;
  }

  /** 返回2倍百搭_出现权重 */
  public int getWild_2_weight() {
    return wild_2_weight;
  }

  /** 返回5倍百搭_出现权重 */
  public int getWild_5_weight() {
    return wild_5_weight;
  }

  /** 返回1倍百搭_至少数量 */
  public int getWild_min() {
    return wild_min;
  }
}
