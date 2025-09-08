package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName CashCowRewards.xlsx
 * @sheetName cashcowrewards
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class CashcowrewardsCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "CashCowRewards.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "cashcowrewards";

  /** 达成条件 */
  protected int condition;
  /** 道具ID_道具数量 */
  protected List<Integer> rewards;

  /** 返回达成条件 */
  public int getCondition() {
    return condition;
  }

  /** 返回道具ID_道具数量 */
  public List<Integer> getRewards() {
    return rewards;
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
