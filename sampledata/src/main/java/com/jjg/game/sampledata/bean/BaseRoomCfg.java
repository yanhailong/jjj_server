package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName BaseRoom.xlsx
 * @sheetName BaseRoom
 * @author Auto.Generator
 * @date 2025年08月16日 15:49:31
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BaseRoomCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "BaseRoom.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "BaseRoom";

  /** 押分倍数 */
  protected List<Integer> betMultiple;
  /** 实际押注抽水入奖池万分比 */
  protected int commissionProp;
  /** 默认押分 */
  protected List<Integer> defaultBet;
  /** 最小准入 */
  protected int enterLimit;
  /** 假累计奖池部分进入比率 */
  protected List<Integer> fakeCommissionProp;
  /** 假总奖池金额 */
  protected long fakePool;
  /** 游戏ID */
  protected int gameType;
  /** 初始化标准池 */
  protected long initBasePool;
  /** 押注进入标准池万分比 */
  protected int initBasePoolProportion;
  /** 单线押分值 */
  protected List<Integer> lineBetScore;
  /** 线注倍数 */
  protected List<Integer> lineMultiple;
  /** 跑马触发金额 */
  protected List<Long> marqueeTrigger;
  /** 上庄最低金额 */
  protected List<Integer> minBankerAmount;
  /** 多语言表ID */
  protected int nameid;
  /** 房间列表 */
  protected List<Integer> room;
  /** 倍场名称 */
  protected int roomName;
  /** VIP等级限制 */
  protected int vipLvLimit;

  /** 返回押分倍数 */
  public List<Integer> getBetMultiple() {
    return betMultiple;
  }

  /** 返回实际押注抽水入奖池万分比 */
  public int getCommissionProp() {
    return commissionProp;
  }

  /** 返回默认押分 */
  public List<Integer> getDefaultBet() {
    return defaultBet;
  }

  /** 返回最小准入 */
  public int getEnterLimit() {
    return enterLimit;
  }

  /** 返回假累计奖池部分进入比率 */
  public List<Integer> getFakeCommissionProp() {
    return fakeCommissionProp;
  }

  /** 返回假总奖池金额 */
  public long getFakePool() {
    return fakePool;
  }

  /** 返回游戏ID */
  public int getGameType() {
    return gameType;
  }

  /** 返回初始化标准池 */
  public long getInitBasePool() {
    return initBasePool;
  }

  /** 返回押注进入标准池万分比 */
  public int getInitBasePoolProportion() {
    return initBasePoolProportion;
  }

  /** 返回单线押分值 */
  public List<Integer> getLineBetScore() {
    return lineBetScore;
  }

  /** 返回线注倍数 */
  public List<Integer> getLineMultiple() {
    return lineMultiple;
  }

  /** 返回跑马触发金额 */
  public List<Long> getMarqueeTrigger() {
    return marqueeTrigger;
  }

  /** 返回上庄最低金额 */
  public List<Integer> getMinBankerAmount() {
    return minBankerAmount;
  }

  /** 返回多语言表ID */
  public int getNameid() {
    return nameid;
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

  @Override
  public boolean equals(Object o) {
    return super.equals(o);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
