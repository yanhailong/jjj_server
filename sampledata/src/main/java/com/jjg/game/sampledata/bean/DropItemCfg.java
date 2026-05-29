package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName dropItem.xlsx
 * @sheetName dropItem
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DropItemCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "dropItem.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "dropItem";

  /** 权重_掉落物品_随机次数 */
  protected List<List<Integer>> DropItem;
  /** 掉落方式 */
  protected int DropType;
  /** 每日掉落次数 */
  protected int dropCount;
  /** 掉落概率 */
  protected int dropTypeProb;
  /** 游戏id */
  protected int gameType;

  /** 返回权重_掉落物品_随机次数 */
  public List<List<Integer>> getDropItem() {
    return DropItem;
  }

  /** 返回掉落方式 */
  public int getDropType() {
    return DropType;
  }

  /** 返回每日掉落次数 */
  public int getDropCount() {
    return dropCount;
  }

  /** 返回掉落概率 */
  public int getDropTypeProb() {
    return dropTypeProb;
  }

  /** 返回游戏id */
  public int getGameType() {
    return gameType;
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
