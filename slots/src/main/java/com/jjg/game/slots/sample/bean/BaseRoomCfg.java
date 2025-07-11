package com.jjg.game.slots.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName BaseRoom.xlsx
 * @sheetName BaseRoom
 * @author Auto.Generator
 * @date 2025年07月11日 11:56:28
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BaseRoomCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "BaseRoom.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "BaseRoom";

  /** 押分系数 */
  protected int betCoefficient;
  /** 押分倍数 */
  protected List<Integer> betMultiple;
  /** 默认押分 */
  protected List<Integer> defaultBet;
  /** 最小准入 */
  protected int enterLimit;
  /** 游戏ID */
  protected int gameType;
  /** 初始化标准池 */
  protected long initBasePool;
  /** 单线押分值 */
  protected List<Integer> lineBetScore;
  /** 线注倍数 */
  protected List<Integer> lineMultiple;
  /** 房间列表 */
  protected List<Integer> room;
  /** 倍场名称 */
  protected int roomName;
  /** VIP等级限制 */
  protected int vipLvLimit;

  /** 返回押分系数 */
  public int getBetCoefficient() {
    return betCoefficient;
  }

  /** 返回押分倍数 */
  public List<Integer> getBetMultiple() {
    return betMultiple;
  }

  /** 返回默认押分 */
  public List<Integer> getDefaultBet() {
    return defaultBet;
  }

  /** 返回最小准入 */
  public int getEnterLimit() {
    return enterLimit;
  }

  /** 返回游戏ID */
  public int getGameType() {
    return gameType;
  }

  /** 返回初始化标准池 */
  public long getInitBasePool() {
    return initBasePool;
  }

  /** 返回单线押分值 */
  public List<Integer> getLineBetScore() {
    return lineBetScore;
  }

  /** 返回线注倍数 */
  public List<Integer> getLineMultiple() {
    return lineMultiple;
  }

  /** 返回房间列表 */
  public List<Integer> getRoom() {
    return room;
  }

  /** 返回倍场名称 */
  public int getRoomName() {
    return roomName;
  }

  /** 返回VIP等级限制 */
  public int getVipLvLimit() {
    return vipLvLimit;
  }
}
