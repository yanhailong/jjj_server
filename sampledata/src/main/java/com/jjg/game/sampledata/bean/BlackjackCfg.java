package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName Blackjack.xlsx
 * @sheetName Blackjack
 * @author Auto.Generator
 * @date 2025年08月20日 13:34:26
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BlackjackCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "Blackjack.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "Blackjack";

  /** 保险 */
  protected int Insurance;
  /** 押分列表 */
  protected List<Integer> betList;
  /** Blackjack */
  protected int blackjack;
  /** 牌副数 */
  protected int decksNum;
  /** 和局 */
  protected int draw;
  /** 爆牌是否收走 */
  protected boolean explodingOut;
  /** 五小龙 */
  protected int fiveLittleDragons;
  /** 游戏ID */
  protected int gameId;
  /** 其他点数 */
  protected int other;
  /** 牌池ID */
  protected int poolId;
  /** 房间类型 */
  protected int roomId;
  /** 21点 */
  protected int twentyOne;

  /** 返回保险 */
  public int getInsurance() {
    return Insurance;
  }

  /** 返回押分列表 */
  public List<Integer> getBetList() {
    return betList;
  }

  /** 返回Blackjack */
  public int getBlackjack() {
    return blackjack;
  }

  /** 返回牌副数 */
  public int getDecksNum() {
    return decksNum;
  }

  /** 返回和局 */
  public int getDraw() {
    return draw;
  }

  /** 返回爆牌是否收走 */
  public boolean getExplodingOut() {
    return explodingOut;
  }

  /** 返回五小龙 */
  public int getFiveLittleDragons() {
    return fiveLittleDragons;
  }

  /** 返回游戏ID */
  public int getGameId() {
    return gameId;
  }

  /** 返回其他点数 */
  public int getOther() {
    return other;
  }

  /** 返回牌池ID */
  public int getPoolId() {
    return poolId;
  }

  /** 返回房间类型 */
  public int getRoomId() {
    return roomId;
  }

  /** 返回21点 */
  public int getTwentyOne() {
    return twentyOne;
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
