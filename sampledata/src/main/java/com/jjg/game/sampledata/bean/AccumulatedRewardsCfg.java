package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName AccumulatedRewards.xlsx
 * @sheetName AccumulatedRewards
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class AccumulatedRewardsCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "AccumulatedRewards.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "AccumulatedRewards";

  /** 累积天数 */
  protected int days;
  /** 签到奖励 */
  protected Map<Integer,Long> getItem;

  /** 返回累积天数 */
  public int getDays() {
    return days;
  }

  /** 返回签到奖励 */
  public Map<Integer,Long> getGetItem() {
    return getItem;
  }

  @Override
  public boolean equals(Object o) {
    return super.equals(o);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
