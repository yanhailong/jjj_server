package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName runninglight.xlsx
 * @sheetName runninglight
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class RunninglightCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "runninglight.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "runninglight";

  /** 中奖扣除万分比 */
  protected int WinRatio;
  /** 押注金额 */
  protected List<Integer> betList;
  /** 游戏id */
  protected int gameID;
  /** 触发跑马灯 */
  protected int marquee;
  /** 多语言id */
  protected int nameid;
  /** 游戏倍场 */
  protected int roomID;
  /** 中奖倍数 */
  protected List<Integer> times;

  /** 返回中奖扣除万分比 */
  public int getWinRatio() {
    return WinRatio;
  }

  /** 返回押注金额 */
  public List<Integer> getBetList() {
    return betList;
  }

  /** 返回游戏id */
  public int getGameID() {
    return gameID;
  }

  /** 返回触发跑马灯 */
  public int getMarquee() {
    return marquee;
  }

  /** 返回多语言id */
  public int getNameid() {
    return nameid;
  }

  /** 返回游戏倍场 */
  public int getRoomID() {
    return roomID;
  }

  /** 返回中奖倍数 */
  public List<Integer> getTimes() {
    return times;
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
