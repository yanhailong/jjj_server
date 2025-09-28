package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName chessTexasStrategy.xlsx
 * @sheetName chessTexasStrategy
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ChessTexasStrategyCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "chessTexasStrategy.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "chessTexasStrategy";

  /** 他人加注的行为权重-我方牌小 */
  protected Map<Integer,Integer> passiveStrategyFailed_2;
  /** 他人加注的行为权重-我方牌小 */
  protected Map<Integer,Integer> passiveStrategyFailed_3;
  /** 他人加注的行为权重-我方牌小 */
  protected Map<Integer,Integer> passiveStrategyFailed_4;
  /** 他人加注的行为权重-我方牌大 */
  protected Map<Integer,Integer> passiveStrategyWin_2;
  /** 他人加注的行为权重-我方牌大 */
  protected Map<Integer,Integer> passiveStrategyWin_3;
  /** 他人加注的行为权重-我方牌大 */
  protected Map<Integer,Integer> passiveStrategyWin_4;
  /** 他人加注的行为权重 */
  protected Map<Integer,Integer> passiveStrategy_1;
  /** 无人加注的行为权重 */
  protected Map<Integer,Integer> proactiveStrategy_1;
  /** 无人加注的行为权重 */
  protected Map<Integer,Integer> proactiveStrategy_2;
  /** 无人加注的行为权重 */
  protected Map<Integer,Integer> proactiveStrategy_3;
  /** 无人加注的行为权重 */
  protected Map<Integer,Integer> proactiveStrategy_4;
  /** 牌值 */
  protected int value;

  /** 返回他人加注的行为权重-我方牌小 */
  public Map<Integer,Integer> getPassiveStrategyFailed_2() {
    return passiveStrategyFailed_2;
  }

  /** 返回他人加注的行为权重-我方牌小 */
  public Map<Integer,Integer> getPassiveStrategyFailed_3() {
    return passiveStrategyFailed_3;
  }

  /** 返回他人加注的行为权重-我方牌小 */
  public Map<Integer,Integer> getPassiveStrategyFailed_4() {
    return passiveStrategyFailed_4;
  }

  /** 返回他人加注的行为权重-我方牌大 */
  public Map<Integer,Integer> getPassiveStrategyWin_2() {
    return passiveStrategyWin_2;
  }

  /** 返回他人加注的行为权重-我方牌大 */
  public Map<Integer,Integer> getPassiveStrategyWin_3() {
    return passiveStrategyWin_3;
  }

  /** 返回他人加注的行为权重-我方牌大 */
  public Map<Integer,Integer> getPassiveStrategyWin_4() {
    return passiveStrategyWin_4;
  }

  /** 返回他人加注的行为权重 */
  public Map<Integer,Integer> getPassiveStrategy_1() {
    return passiveStrategy_1;
  }

  /** 返回无人加注的行为权重 */
  public Map<Integer,Integer> getProactiveStrategy_1() {
    return proactiveStrategy_1;
  }

  /** 返回无人加注的行为权重 */
  public Map<Integer,Integer> getProactiveStrategy_2() {
    return proactiveStrategy_2;
  }

  /** 返回无人加注的行为权重 */
  public Map<Integer,Integer> getProactiveStrategy_3() {
    return proactiveStrategy_3;
  }

  /** 返回无人加注的行为权重 */
  public Map<Integer,Integer> getProactiveStrategy_4() {
    return proactiveStrategy_4;
  }

  /** 返回牌值 */
  public int getValue() {
    return value;
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
