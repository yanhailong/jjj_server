package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName warehouse.xlsx
 * @sheetName Warehouse
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class WarehouseCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "warehouse.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "Warehouse";

  /** 倍场显示默认押注 */
  protected int betShow;
  /** 最小准入 */
  protected int enterLimit;
  /** 最大准入分数 */
  protected int enterMax;
  /** 游戏ID */
  protected int gameID;
  /** 游戏分类 */
  protected int gameType;
  /** 多语言表ID */
  protected int nameid;
  /** 房间最大人数:达到X人时创建新房间 */
  protected String participants_max;
  /** 角色等级限制 */
  protected int playerLvLimit;
  /** 最少保留房间数量：无人后间隔X秒删除 */
  protected List<Integer> roomDeletion_Solution;
  /** 说明 */
  protected int roomType;
  /** 交易项目ID */
  protected int transactionItemId;

  /** 返回倍场显示默认押注 */
  public int getBetShow() {
    return betShow;
  }

  /** 返回最小准入 */
  public int getEnterLimit() {
    return enterLimit;
  }

  /** 返回最大准入分数 */
  public int getEnterMax() {
    return enterMax;
  }

  /** 返回游戏ID */
  public int getGameID() {
    return gameID;
  }

  /** 返回游戏分类 */
  public int getGameType() {
    return gameType;
  }

  /** 返回多语言表ID */
  public int getNameid() {
    return nameid;
  }

  /** 返回房间最大人数:达到X人时创建新房间 */
  public String getParticipants_max() {
    return participants_max;
  }

  /** 返回角色等级限制 */
  public int getPlayerLvLimit() {
    return playerLvLimit;
  }

  /** 返回最少保留房间数量：无人后间隔X秒删除 */
  public List<Integer> getRoomDeletion_Solution() {
    return roomDeletion_Solution;
  }

  /** 返回说明 */
  public int getRoomType() {
    return roomType;
  }

  /** 返回交易项目ID */
  public int getTransactionItemId() {
    return transactionItemId;
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
