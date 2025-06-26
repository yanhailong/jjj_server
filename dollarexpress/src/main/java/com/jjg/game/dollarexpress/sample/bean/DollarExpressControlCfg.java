package com.jjg.game.dollarexpress.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName dollarExpressControl.xlsx
 * @sheetName DollarExpressControl
 * @author Auto.Generator
 * @date 2025年06月24日 16:33:11
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DollarExpressControlCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "dollarExpressControl.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "DollarExpressControl";

  /** 常规赔率权重 */
  protected List<Integer> axleList;
  /** 进入最高条件 */
  protected long entryConditionMax;
  /** 进入最低条件 */
  protected long entryConditionMin;
  /** 含3个以上免费 */
  protected int free;
  /** 最后一列含金火车 */
  protected int goldTrain;
  /** 最后一列含保险箱 */
  protected int safeBox;
  /** 中奖图标含四色火车图标 */
  protected int train;

  /** 返回常规赔率权重 */
  public List<Integer> getAxleList() {
    return axleList;
  }

  /** 返回进入最高条件 */
  public long getEntryConditionMax() {
    return entryConditionMax;
  }

  /** 返回进入最低条件 */
  public long getEntryConditionMin() {
    return entryConditionMin;
  }

  /** 返回含3个以上免费 */
  public int getFree() {
    return free;
  }

  /** 返回最后一列含金火车 */
  public int getGoldTrain() {
    return goldTrain;
  }

  /** 返回最后一列含保险箱 */
  public int getSafeBox() {
    return safeBox;
  }

  /** 返回中奖图标含四色火车图标 */
  public int getTrain() {
    return train;
  }
}
