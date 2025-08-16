package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName warehouse.xlsx
 * @sheetName Warehouse
 * @author Auto.Generator
 * @date 2025年08月16日 15:49:31
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
  /** 多语言表ID */
  protected int nameid;
  /** 房间最大人数:达到X人时创建新房间 */
  protected String participants_max;
  /** 最少保留房间数量：无人后间隔X秒删除 */
  protected List<Integer> roomDeletion_Solution;
  /** 说明 */
  protected int roomType;
  /** VIP等级限制 */
  protected int vipLvLimit;

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

  /** 返回多语言表ID */
  public int getNameid() {
    return nameid;
  }

  /** 返回房间最大人数:达到X人时创建新房间 */
  public String getParticipants_max() {
    return participants_max;
  }

  /** 返回最少保留房间数量：无人后间隔X秒删除 */
  public List<Integer> getRoomDeletion_Solution() {
    return roomDeletion_Solution;
  }

  /** 返回说明 */
  public int getRoomType() {
    return roomType;
  }

  /** 返回VIP等级限制 */
  public int getVipLvLimit() {
    return vipLvLimit;
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
