package com.jjg.game.poker.game.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName PokerPool.xlsx
 * @sheetName PokerPool
 * @author Auto.Generator
 * @date 2025年08月05日 11:38:22
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PokerPoolCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "PokerPool.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "PokerPool";

  /** 点数1 */
  protected String points;
  /** 点数1大小 */
  protected int pointsNum;
  /** 牌池ID */
  protected int poolId;
  /** 点数2 */
  protected String suit;
  /** 点数2大小 */
  protected int suitNum;

  /** 返回点数1 */
  public String getPoints() {
    return points;
  }

  /** 返回点数1大小 */
  public int getPointsNum() {
    return pointsNum;
  }

  /** 返回牌池ID */
  public int getPoolId() {
    return poolId;
  }

  /** 返回点数2 */
  public String getSuit() {
    return suit;
  }

  /** 返回点数2大小 */
  public int getSuitNum() {
    return suitNum;
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
