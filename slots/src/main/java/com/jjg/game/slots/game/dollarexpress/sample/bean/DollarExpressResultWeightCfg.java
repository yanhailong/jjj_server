package com.jjg.game.slots.game.dollarexpress.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName dollarExpressResultWeight.xlsx
 * @sheetName DollarExpressResultWeight
 * @author Auto.Generator
 * @date 2025年06月27日 09:57:50
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DollarExpressResultWeightCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "dollarExpressResultWeight.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "DollarExpressResultWeight";

  /** 免费模式-选普通火车 */
  protected int freeToTrainWeight;
  /** 免费模式 */
  protected int freeWeight;
  /** 金火车 */
  protected int goldTrainWeight;
  /** 常规图标赔付上限 */
  protected String payoutMax;
  /** 常规图标赔付下限 */
  protected String payoutMin;
  /** 保险箱 */
  protected int safeBoxWeight;
  /** 拉火车 */
  protected int trainWeight;
  /** 常规赔率权重 */
  protected List<Integer> weights;
  /** 含百搭图标 */
  protected int wildWeight;

  /** 返回免费模式-选普通火车 */
  public int getFreeToTrainWeight() {
    return freeToTrainWeight;
  }

  /** 返回免费模式 */
  public int getFreeWeight() {
    return freeWeight;
  }

  /** 返回金火车 */
  public int getGoldTrainWeight() {
    return goldTrainWeight;
  }

  /** 返回常规图标赔付上限 */
  public String getPayoutMax() {
    return payoutMax;
  }

  /** 返回常规图标赔付下限 */
  public String getPayoutMin() {
    return payoutMin;
  }

  /** 返回保险箱 */
  public int getSafeBoxWeight() {
    return safeBoxWeight;
  }

  /** 返回拉火车 */
  public int getTrainWeight() {
    return trainWeight;
  }

  /** 返回常规赔率权重 */
  public List<Integer> getWeights() {
    return weights;
  }

  /** 返回含百搭图标 */
  public int getWildWeight() {
    return wildWeight;
  }
}
