package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName WinPosWeight.xlsx
 * @sheetName WinPosWeight
 * @author Auto.Generator
 * @date 2025年08月19日 11:30:38
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class WinPosWeightCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "WinPosWeight.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "WinPosWeight";

  /** 对应下注区中奖位置 */
  protected List<Integer> betArea;
  /** 游戏ID */
  protected int gameID;
  /** 是否抽水 */
  protected int isRatio;
  /** 赔付倍数 */
  protected int odds;
  /** 中奖权重值 */
  protected int posWeight;
  /** 返还押分比例 */
  protected int returnRate;
  /** 小游戏触发ID */
  protected Map<Long,Integer> trigID;
  /** 位置序列 */
  protected int winPosID;
  /** 奖励类型 */
  protected int winType;

  /** 返回对应下注区中奖位置 */
  public List<Integer> getBetArea() {
    return betArea;
  }

  /** 返回游戏ID */
  public int getGameID() {
    return gameID;
  }

  /** 返回是否抽水 */
  public int getIsRatio() {
    return isRatio;
  }

  /** 返回赔付倍数 */
  public int getOdds() {
    return odds;
  }

  /** 返回中奖权重值 */
  public int getPosWeight() {
    return posWeight;
  }

  /** 返回返还押分比例 */
  public int getReturnRate() {
    return returnRate;
  }

  /** 返回小游戏触发ID */
  public Map<Long,Integer> getTrigID() {
    return trigID;
  }

  /** 返回位置序列 */
  public int getWinPosID() {
    return winPosID;
  }

  /** 返回奖励类型 */
  public int getWinType() {
    return winType;
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
