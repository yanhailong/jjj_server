package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName SeasonGemDrop.xlsx
 * @sheetName SeasonGemDrop
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SeasonGemDropCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "SeasonGemDrop.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "SeasonGemDrop";

  /** 权重_掉落物品_随机次数 */
  protected List<List<Integer>> DropItem;
  /** 掉落概率(万分比） */
  protected int DropRate;
  /** 赛季ID */
  protected int SeasonID;
  /** 每日掉落次数上限 */
  protected int dropCount;

  /** 返回权重_掉落物品_随机次数 */
  public List<List<Integer>> getDropItem() {
    return DropItem;
  }

  /** 返回掉落概率(万分比） */
  public int getDropRate() {
    return DropRate;
  }

  /** 返回赛季ID */
  public int getSeasonID() {
    return SeasonID;
  }

  /** 返回每日掉落次数上限 */
  public int getDropCount() {
    return dropCount;
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
