package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName SeasonGemCraft.xlsx
 * @sheetName SeasonGemCraft
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SeasonGemCraftCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "SeasonGemCraft.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "SeasonGemCraft";

  /** 消耗的数量 */
  protected int CostAmount;
  /** 合成失败宝石保留的数量 */
  protected int FailKeepAmount;
  /** 合成赛季币消耗 */
  protected int MergeCost;
  /** 合成成功概率(百分比） */
  protected int MergeSuccessRate;
  /** 合成成功的宝石 */
  protected List<List<Integer>> SuccessGem;
  /** 合成消耗的宝石品质 */
  protected int SynthesisGemQuality;

  /** 返回消耗的数量 */
  public int getCostAmount() {
    return CostAmount;
  }

  /** 返回合成失败宝石保留的数量 */
  public int getFailKeepAmount() {
    return FailKeepAmount;
  }

  /** 返回合成赛季币消耗 */
  public int getMergeCost() {
    return MergeCost;
  }

  /** 返回合成成功概率(百分比） */
  public int getMergeSuccessRate() {
    return MergeSuccessRate;
  }

  /** 返回合成成功的宝石 */
  public List<List<Integer>> getSuccessGem() {
    return SuccessGem;
  }

  /** 返回合成消耗的宝石品质 */
  public int getSynthesisGemQuality() {
    return SynthesisGemQuality;
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
