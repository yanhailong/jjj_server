package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName playerLevelConfig.xlsx
 * @sheetName playerLevelConfig
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PlayerLevelConfigCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "playerLevelConfig.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "playerLevelConfig";

  /** 黑名单数量 */
  protected int blockListNum;
  /** 掉落ID在dropConfig */
  protected int dropConfig;
  /** 基础关注数量 */
  protected int friendsNum;
  /** 道具奖励 */
  protected Map<Integer,Long> getItem;
  /** 升级所需经验 */
  protected long levelUpExp;
  /** 额外流水系数 */
  protected int prop;
  /** 同时创建游戏数量上限 */
  protected int roomNum;

  /** 返回黑名单数量 */
  public int getBlockListNum() {
    return blockListNum;
  }

  /** 返回掉落ID在dropConfig */
  public int getDropConfig() {
    return dropConfig;
  }

  /** 返回基础关注数量 */
  public int getFriendsNum() {
    return friendsNum;
  }

  /** 返回道具奖励 */
  public Map<Integer,Long> getGetItem() {
    return getItem;
  }

  /** 返回升级所需经验 */
  public long getLevelUpExp() {
    return levelUpExp;
  }

  /** 返回额外流水系数 */
  public int getProp() {
    return prop;
  }

  /** 返回同时创建游戏数量上限 */
  public int getRoomNum() {
    return roomNum;
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
