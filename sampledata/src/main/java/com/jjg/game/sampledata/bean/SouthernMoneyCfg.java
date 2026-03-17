package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName SouthernMoney.xlsx
 * @sheetName SouthernMoney
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SouthernMoneyCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "SouthernMoney.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "SouthernMoney";

  /** 押分列表 */
  protected int betList;
  /** 游戏ID */
  protected int gameId;
  /** 牌池ID */
  protected int poolId;
  /** 被管炸弹的倍数（三连对） */
  protected int remainBoom;
  /** 被管炸弹的倍数（四条） */
  protected int fourkindboom;
  /** 被管炸弹的倍数（四连对） */
  protected int fourpairsboom;
  /** 结算剩余普通牌的倍数 */
  protected int remainNormal;
  /** 结算剩余手牌黑2的倍数 */
  protected int remainblack2;
  /** 结算剩余炸弹的倍数（三连对） */
  protected int remainBoom1;
  /** 结算剩余炸弹的倍数（四条） */
  protected int fourkindboom1;
  /** 结算剩余手牌红2的倍数 */
  protected int remainred2;
  /** 房间类型 */
  protected int roomId;

  /** 返回押分列表 */
  public int getBetList() {
    return betList;
  }

  /** 返回游戏ID */
  public int getGameId() {
    return gameId;
  }

  /** 返回牌池ID */
  public int getPoolId() {
    return poolId;
  }

  /** 返回被管炸弹的倍数（三连对） */
  public int getRemainBoom() {
    return remainBoom;
  }

  /** 返回被管炸弹的倍数（四条） */
  public int getFourkindboom() {
    return fourkindboom;
  }

  /** 返回被管炸弹的倍数（四连对） */
  public int getFourpairsboom() {
    return fourpairsboom;
  }

  /** 返回结算剩余普通牌的倍数 */
  public int getRemainNormal() {
    return remainNormal;
  }

  /** 返回结算剩余手牌黑2的倍数 */
  public int getRemainblack2() {
    return remainblack2;
  }

  /** 返回结算剩余炸弹的倍数（三连对） */
  public int getRemainBoom1() {
    return remainBoom1;
  }

  /** 返回结算剩余炸弹的倍数（四条） */
  public int getFourkindboom1() {
    return fourkindboom1;
  }

  /** 返回结算剩余手牌红2的倍数 */
  public int getRemainred2() {
    return remainred2;
  }

  /** 返回房间类型 */
  public int getRoomId() {
    return roomId;
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
