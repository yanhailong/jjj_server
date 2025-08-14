package com.jjg.game.hall.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName vipLevel.xlsx
 * @sheetName VipLevelConfig
 * @author Auto.Generator
 * @date 2025年08月11日 16:24:58
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class VipLevelConfigCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "vipLevel.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "VipLevelConfig";

  /** 基础关注数量 */
  protected int friendsNum;
  /** 升级所需经验 */
  protected int levelUpExp;
  /** 解锁玩家装扮 */
  protected int playerDress;
  /** 额外流水系数 */
  protected int prop;
  /** 同时创建游戏数量上限 */
  protected int roomNum;

  /** 返回基础关注数量 */
  public int getFriendsNum() {
    return friendsNum;
  }

  /** 返回升级所需经验 */
  public int getLevelUpExp() {
    return levelUpExp;
  }

  /** 返回解锁玩家装扮 */
  public int getPlayerDress() {
    return playerDress;
  }

  /** 返回额外流水系数 */
  public int getProp() {
    return prop;
  }

  /** 返回同时创建游戏数量上限 */
  public int getRoomNum() {
    return roomNum;
  }
}
