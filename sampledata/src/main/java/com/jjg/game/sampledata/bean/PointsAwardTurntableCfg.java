package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName PointsAwardTurntable.xlsx
 * @sheetName PointsAwardTurntable
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PointsAwardTurntableCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "PointsAwardTurntable.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "PointsAwardTurntable";

  /** 权重(万分比) */
  protected int Probability;
  /** 道具奖励 */
  protected List<Integer> getItem;
  /** 转盘格子 */
  protected int grid;
  /** 积分ICON */
  protected String integralIcon;
  /** 积分奖励 */
  protected int integralNum;
  /** 开启时间 */
  protected String time;

  /** 返回权重(万分比) */
  public int getProbability() {
    return Probability;
  }

  /** 返回道具奖励 */
  public List<Integer> getGetItem() {
    return getItem;
  }

  /** 返回转盘格子 */
  public int getGrid() {
    return grid;
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
