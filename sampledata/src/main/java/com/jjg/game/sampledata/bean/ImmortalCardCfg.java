package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName ImmortalCard.xlsx
 * @sheetName ImmortalCard
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ImmortalCardCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "ImmortalCard.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "ImmortalCard";

  /** 游戏id */
  protected int gameID;
  /** 房间类型 */
  protected int roomID;
  /** 房间底分 */
  protected int betList;
  /** 牌池id */
  protected int poolId;
  /** 场次最小输赢值 */
  protected int WinLoss;
  /** 结算封顶值 */
  protected int MaxCap;
  /** 回合倍率 */
  protected Map<Integer,Integer> RoundMultiplier;

  /** 返回游戏id */
  public int getGameID() {
    return gameID;
  }

  /** 返回房间类型 */
  public int getRoomID() {
    return roomID;
  }

  /** 返回房间底分 */
  public int getBetList() {
    return betList;
  }

  /** 返回牌池id */
  public int getPoolId() {
    return poolId;
  }

  /** 返回场次最小输赢值 */
  public int getWinLoss() {
    return WinLoss;
  }

  /** 返回结算封顶值 */
  public int getMaxCap() {
    return MaxCap;
  }

  /** 返回回合倍率 */
  public Map<Integer,Integer> getRoundMultiplier() {
    return RoundMultiplier;
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
