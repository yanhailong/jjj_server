package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName MiningMapGeneration.xlsx
 * @sheetName MiningMapGeneration
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class MiningMapGenerationCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "MiningMapGeneration.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "MiningMapGeneration";

  /** 固定格子 */
  protected List<List<Integer>> FixedGrid;
  /** 高度（米） */
  protected int Height;
  /** 随机生成 */
  protected List<List<Integer>> Randomizedgrid;
  /** 总高度（米） */
  protected int TotalHeight;

  /** 返回固定格子 */
  public List<List<Integer>> getFixedGrid() {
    return FixedGrid;
  }

  /** 返回高度（米） */
  public int getHeight() {
    return Height;
  }

  /** 返回随机生成 */
  public List<List<Integer>> getRandomizedgrid() {
    return Randomizedgrid;
  }

  /** 返回总高度（米） */
  public int getTotalHeight() {
    return TotalHeight;
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
