package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName SeasonGem.xlsx
 * @sheetName SeasonGem
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SeasonGemCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "SeasonGem.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "SeasonGem";

  /** 宝石描述多语言 */
  protected int GemDesc;
  /** 宝石名称多语言 */
  protected int GemName;
  /** 流派 */
  protected int Genre;
  /** 品质 */
  protected int Rarity;
  /** 金额解锁 */
  protected List<Long> bet;
  /** 游戏ID */
  protected int gameID;
  /** 道具ID */
  protected int itemId;
  /** 进入特殊模式概率提升 */
  protected Map<Integer,Integer> specialMode;
  /** 特殊模式中奖概率提升 */
  protected Map<Integer,Map<Integer,Integer>> specialModeProbUp;
  /** 类型 */
  protected int type;
  /** 中奖概率提升 */
  protected Map<Integer,Map<Integer,Integer>> winRate;

  /** 返回宝石描述多语言 */
  public int getGemDesc() {
    return GemDesc;
  }

  /** 返回宝石名称多语言 */
  public int getGemName() {
    return GemName;
  }

  /** 返回流派 */
  public int getGenre() {
    return Genre;
  }

  /** 返回品质 */
  public int getRarity() {
    return Rarity;
  }

  /** 返回金额解锁 */
  public List<Long> getBet() {
    return bet;
  }

  /** 返回游戏ID */
  public int getGameID() {
    return gameID;
  }

  /** 返回道具ID */
  public int getItemId() {
    return itemId;
  }

  /** 返回进入特殊模式概率提升 */
  public Map<Integer,Integer> getSpecialMode() {
    return specialMode;
  }

  /** 返回特殊模式中奖概率提升 */
  public Map<Integer,Map<Integer,Integer>> getSpecialModeProbUp() {
    return specialModeProbUp;
  }

  /** 返回类型 */
  public int getType() {
    return type;
  }

  /** 返回中奖概率提升 */
  public Map<Integer,Map<Integer,Integer>> getWinRate() {
    return winRate;
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
