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

  /** 是否自动完成 */
  protected int autoCompletion;
  /** 押分倍数 */
  protected List<Integer> betMultiple;
  /** 初始列数 */
  protected int cols;
  /** 线注倍数 */
  protected List<Integer> lineMultiple;
  /** 中奖线类型 */
  protected int lineType;
  /** 最大线数 */
  protected int maxLine;
  /** 名字多语言ID */
  protected int name;
  /** 奖池规则ID */
  protected List<Integer> prizePoolIdList;
  /** 初始行数 */
  protected int rows;
  /** 消除类型 */
  protected int type;

  /** 返回是否自动完成 */
  public int getAutoCompletion() {
    return autoCompletion;
  }

  /** 返回押分倍数 */
  public List<Integer> getBetMultiple() {
    return betMultiple;
  }

  /** 返回初始列数 */
  public int getCols() {
    return cols;
  }

  /** 返回线注倍数 */
  public List<Integer> getLineMultiple() {
    return lineMultiple;
  }

  /** 返回中奖线类型 */
  public int getLineType() {
    return lineType;
  }

  /** 返回最大线数 */
  public int getMaxLine() {
    return maxLine;
  }

  /** 返回名字多语言ID */
  public int getName() {
    return name;
  }

  /** 返回奖池规则ID */
  public List<Integer> getPrizePoolIdList() {
    return prizePoolIdList;
  }

  /** 返回初始行数 */
  public int getRows() {
    return rows;
  }

  /** 返回消除类型 */
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
