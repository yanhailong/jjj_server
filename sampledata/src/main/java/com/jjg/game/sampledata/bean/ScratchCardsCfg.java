package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName ScratchCards.xlsx
 * @sheetName ScratchCards
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ScratchCardsCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "ScratchCards.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "ScratchCards";

  /** 奖励1 */
  protected Map<Integer,Integer> getitem;
  /** 中奖图标数量 */
  protected int name;
  /** 中奖权重值 */
  protected int weight;

  /** 返回奖励1 */
  public Map<Integer,Integer> getGetitem() {
    return getitem;
  }

  /** 返回中奖图标数量 */
  public int getName() {
    return name;
  }

  /** 返回中奖权重值 */
  public int getWeight() {
    return weight;
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
