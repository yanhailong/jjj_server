package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName DailyRewards.xlsx
 * @sheetName DailyRewards
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DailyRewardsCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "DailyRewards.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "DailyRewards";

  /** 签到天数 */
  protected int days;
  /** 签到奖励 */
  protected Map<Integer,Long> getItem;
  /** 类型 */
  protected int type;

  /** 返回签到天数 */
  public int getDays() {
    return days;
  }

  /** 返回签到奖励 */
  public Map<Integer,Long> getGetItem() {
    return getItem;
  }

  /** 返回类型 */
  public int getType() {
    return type;
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
