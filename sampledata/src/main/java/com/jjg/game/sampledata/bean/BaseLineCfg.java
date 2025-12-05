package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName BaseLine.xlsx
 * @sheetName BaseLine
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BaseLineCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "BaseLine.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "BaseLine";

  /** 方向算法 */
  protected List<Integer> direction;
  /** 指定适用模式 */
  protected int gameMode;
  /** 玩法算法 */
  protected int gamePlayCalc;
  /** 游戏ID */
  protected int gameType;
  /** 线路ID */
  protected int lineId;
  /** 位置坐标 */
  protected List<Integer> posLocation;

  /** 返回方向算法 */
  public List<Integer> getDirection() {
    return direction;
  }

  /** 返回指定适用模式 */
  public int getGameMode() {
    return gameMode;
  }

  /** 返回玩法算法 */
  public int getGamePlayCalc() {
    return gamePlayCalc;
  }

  /** 返回游戏ID */
  public int getGameType() {
    return gameType;
  }

  /** 返回线路ID */
  public int getLineId() {
    return lineId;
  }

  /** 返回位置坐标 */
  public List<Integer> getPosLocation() {
    return posLocation;
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
