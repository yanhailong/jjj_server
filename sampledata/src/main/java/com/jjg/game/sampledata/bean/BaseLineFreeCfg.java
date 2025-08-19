package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName BaseLineFree.xlsx
 * @sheetName BaseLineFree
 * @author Auto.Generator
 * @date 2025年08月19日 11:30:38
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BaseLineFreeCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "BaseLineFree.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "BaseLineFree";

  /** 双向 */
  protected Map<Integer,Boolean> Multiple;
  /** 指定元素列表 */
  protected Map<Integer,List<Integer>> appointElementList;
  /** 额外投注双向 */
  protected Map<Integer,List<Integer>> extraBetMultiple;
  /** 玩法 */
  protected Map<Integer,Integer> gamePlay;
  /** 玩法算法 */
  protected int gamePlayCalc;
  /** 游戏ID */
  protected int gameType;
  /** 最少元素种类 */
  protected List<List<Integer>> leastElementKind;
  /** 线路ID */
  protected int lineId;
  /** 线路类型 */
  protected int lineType;
  /** 位置坐标 */
  protected List<Map<Integer,Integer>> posLocation;

  /** 返回双向 */
  public Map<Integer,Boolean> getMultiple() {
    return Multiple;
  }

  /** 返回指定元素列表 */
  public Map<Integer,List<Integer>> getAppointElementList() {
    return appointElementList;
  }

  /** 返回额外投注双向 */
  public Map<Integer,List<Integer>> getExtraBetMultiple() {
    return extraBetMultiple;
  }

  /** 返回玩法 */
  public Map<Integer,Integer> getGamePlay() {
    return gamePlay;
  }

  /** 返回玩法算法 */
  public int getGamePlayCalc() {
    return gamePlayCalc;
  }

  /** 返回游戏ID */
  public int getGameType() {
    return gameType;
  }

  /** 返回最少元素种类 */
  public List<List<Integer>> getLeastElementKind() {
    return leastElementKind;
  }

  /** 返回线路ID */
  public int getLineId() {
    return lineId;
  }

  /** 返回线路类型 */
  public int getLineType() {
    return lineType;
  }

  /** 返回位置坐标 */
  public List<Map<Integer,Integer>> getPosLocation() {
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
