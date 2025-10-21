package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName ChessJackStrategy.xlsx
 * @sheetName chessJackStrategy
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ChessJackStrategyCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "ChessJackStrategy.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "chessJackStrategy";

  /** 行为权重1加倍2要牌3停牌4分牌概率 */
  protected Map<Integer,Integer> Strategy;
  /** 是否有A */
  protected int type;
  /** 牌值,A已1分记 */
  protected int value;

  /** 返回行为权重1加倍2要牌3停牌4分牌概率 */
  public Map<Integer,Integer> getStrategy() {
    return Strategy;
  }

  /** 返回是否有A */
  public int getType() {
    return type;
  }

  /** 返回牌值,A已1分记 */
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
