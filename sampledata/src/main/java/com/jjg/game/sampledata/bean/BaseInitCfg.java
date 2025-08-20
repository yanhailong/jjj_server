package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName BaseInit.xlsx
 * @sheetName BaseInit
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BaseInitCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "BaseInit.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "BaseInit";

  /** 额外投注倍率 */
  protected int additionalBetRatio;
  /** 是否自动完成 */
  protected int autoCompletion;
  /** 列数 */
  protected int cols;
  /** 分散使用格子 */
  protected List<Integer> distributeGrid;
  /** ENGNAME */
  protected String engName;
  /** 图标 */
  protected String icon;
  /** 最大线数 */
  protected int maxLine;
  /** 最小线数 */
  protected int minLine;
  /** 名字 */
  protected String name;
  /** 奖池ID */
  protected List<Integer> prizePoolIdList;
  /** 行数 */
  protected int rows;
  /** 类型 */
  protected int type;

  /** 返回额外投注倍率 */
  public int getAdditionalBetRatio() {
    return additionalBetRatio;
  }

  /** 返回是否自动完成 */
  public int getAutoCompletion() {
    return autoCompletion;
  }

  /** 返回列数 */
  public int getCols() {
    return cols;
  }

  /** 返回分散使用格子 */
  public List<Integer> getDistributeGrid() {
    return distributeGrid;
  }

  /** 返回ENGNAME */
  public String getEngName() {
    return engName;
  }

  /** 返回图标 */
  public String getIcon() {
    return icon;
  }

  /** 返回最大线数 */
  public int getMaxLine() {
    return maxLine;
  }

  /** 返回最小线数 */
  public int getMinLine() {
    return minLine;
  }

  /** 返回名字 */
  public String getName() {
    return name;
  }

  /** 返回奖池ID */
  public List<Integer> getPrizePoolIdList() {
    return prizePoolIdList;
  }

  /** 返回行数 */
  public int getRows() {
    return rows;
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
