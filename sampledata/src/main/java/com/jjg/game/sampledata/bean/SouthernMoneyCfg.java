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
  /** 牌局中炸弹倍数 */
  protected List<Integer> boomMultiple;
  /** 游戏ID */
  protected int gameId;
  /** 牌池ID */
  protected int poolId;
  /** 结算剩余手牌2的倍数 */
  protected int remain2;
  /** 结算剩余炸弹的倍数 */
  protected int remainBoom;
  /** 结算剩余普通牌的倍数 */
  protected int remainNormal;
  /** 房间类型 */
  protected int roomId;

  /** 返回押分列表 */
  public int getBetList() {
    return betList;
  }

  /** 返回牌局中炸弹倍数 */
  public List<Integer> getBoomMultiple() {
    return boomMultiple;
  }

  /** 返回游戏ID */
  public int getGameId() {
    return gameId;
  }

  /** 返回牌池ID */
  public int getPoolId() {
    return poolId;
  }

  /** 返回结算剩余手牌2的倍数 */
  public int getRemain2() {
    return remain2;
  }

  /** 返回结算剩余炸弹的倍数 */
  public int getRemainBoom() {
    return remainBoom;
  }

  /** 返回结算剩余普通牌的倍数 */
  public int getRemainNormal() {
    return remainNormal;
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
