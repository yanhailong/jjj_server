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

  /** 转盘奖励 */
  protected List<Integer> getitem;
  /** 转盘格子 */
  protected int grid;

  /** 返回转盘奖励 */
  public List<Integer> getGetitem() {
    return getitem;
  }

  /** 返回转盘格子 */
  public int getGrid() {
    return grid;
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
