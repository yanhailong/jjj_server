package com.jjg.game.sampledata.bean;

import javax.annotation.processing.Generated;
import java.util.List;
/**
 * 配置bean
 *
 * @excelName Texas.xlsx
 * @sheetName Texas
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class TexasCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "Texas.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "Texas";

  /** 前注 */
  protected int ante;
  /** 加注大盲倍数 */
  protected List<Integer> bbAddMulti;
  /** 大盲 */
  protected int bbNum;
  /** 销牌数量 */
  protected int burnNum;
  /** 默认入座金币 */
  protected int coinsNum;
  /** 请离房间（毫秒） */
  protected int escTime;
  /** 踢出房间提示 */
  protected int escTipText;
  /** 游戏ID */
  protected int gameID;
  /** 保险赔率 */
  protected List<Integer> insuranceOdd;
  /** 牌池ID */
  protected int pokerPool;
  /** 加注底池操作 */
  protected String pool_OpType;
  /** 加注方式 */
  protected int raiseWayType;
  /** 房间类型 */
  protected int roomID;
  /** 小盲 */
  protected int sbNum;
  /** 结算后看牌 */
  protected boolean showDown;
  /** 上桌金额 */
  protected int tablecoin;

  /** 返回前注 */
  public int getAnte() {
    return ante;
  }

  /** 返回加注大盲倍数 */
  public List<Integer> getBbAddMulti() {
    return bbAddMulti;
  }

  /** 返回大盲 */
  public int getBbNum() {
    return bbNum;
  }

  /** 返回销牌数量 */
  public int getBurnNum() {
    return burnNum;
  }

  /** 返回默认入座金币 */
  public int getCoinsNum() {
    return coinsNum;
  }

  /** 返回请离房间（毫秒） */
  public int getEscTime() {
    return escTime;
  }

  /** 返回踢出房间提示 */
  public int getEscTipText() {
    return escTipText;
  }

  /** 返回游戏ID */
  public int getGameID() {
    return gameID;
  }

  /** 返回保险赔率 */
  public List<Integer> getInsuranceOdd() {
    return insuranceOdd;
  }

  /** 返回牌池ID */
  public int getPokerPool() {
    return pokerPool;
  }

  /** 返回加注底池操作 */
  public String getPool_OpType() {
    return pool_OpType;
  }

  /** 返回加注方式 */
  public int getRaiseWayType() {
    return raiseWayType;
  }

  /** 返回房间类型 */
  public int getRoomID() {
    return roomID;
  }

  /** 返回小盲 */
  public int getSbNum() {
    return sbNum;
  }

  /** 返回结算后看牌 */
  public boolean getShowDown() {
    return showDown;
  }

  /** 返回上桌金额 */
  public int getTablecoin() {
    return tablecoin;
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
