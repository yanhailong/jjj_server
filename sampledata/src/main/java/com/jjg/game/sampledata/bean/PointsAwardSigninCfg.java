package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName PointsAwardSignin.xlsx
 * @sheetName PointsAwardSignin
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PointsAwardSigninCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "PointsAwardSignin.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "PointsAwardSignin";

  /** 签到天数 */
  protected int day;
  /** 道具奖励 */
  protected List<Integer> getItem;
  /** 积分ICON */
  protected String integralIcon;
  /** 积分奖励 */
  protected int integralNum;
  /** 开启时间 */
  protected String time;

  /** 返回签到天数 */
  public int getDay() {
    return day;
  }

  /** 返回道具奖励 */
  public List<Integer> getGetItem() {
    return getItem;
  }

  /** 返回积分ICON */
  public String getIntegralIcon() {
    return integralIcon;
  }

  /** 返回积分奖励 */
  public int getIntegralNum() {
    return integralNum;
  }

  /** 返回开启时间 */
  public String getTime() {
    return time;
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
