package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName BetArea.xlsx
 * @sheetName BetArea
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BetAreaCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "BetArea.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "BetArea";

  /** 下注区位置 */
  protected int areaID;
  /** 基础赔付倍数 */
  protected int baseBet;
  /** 基础返还下注比例 */
  protected int baseReturnRate;
  /** 游戏ID */
  protected int gameID;
  /** 当前区域最大赔付倍数 */
  protected int maxPetMultiplier;
  /** 对应开奖位置列表 */
  protected List<Integer> posWin;
  /** 下注前置区域 */
  protected List<Integer> preBetArea;
  /** 互斥组ID */
  protected int repulsionID;
  /** 总下注上限倍数-个人 */
  protected int tbPlayerUpperLimit;
  /** 总下注上限倍数 */
  protected int tbUpperLimit;

  /** 返回下注区位置 */
  public int getAreaID() {
    return areaID;
  }

  /** 返回基础赔付倍数 */
  public int getBaseBet() {
    return baseBet;
  }

  /** 返回基础返还下注比例 */
  public int getBaseReturnRate() {
    return baseReturnRate;
  }

  /** 返回游戏ID */
  public int getGameID() {
    return gameID;
  }

  /** 返回当前区域最大赔付倍数 */
  public int getMaxPetMultiplier() {
    return maxPetMultiplier;
  }

  /** 返回对应开奖位置列表 */
  public List<Integer> getPosWin() {
    return posWin;
  }

  /** 返回下注前置区域 */
  public List<Integer> getPreBetArea() {
    return preBetArea;
  }

  /** 返回互斥组ID */
  public int getRepulsionID() {
    return repulsionID;
  }

  /** 返回总下注上限倍数-个人 */
  public int getTbPlayerUpperLimit() {
    return tbPlayerUpperLimit;
  }

  /** 返回总下注上限倍数 */
  public int getTbUpperLimit() {
    return tbUpperLimit;
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
