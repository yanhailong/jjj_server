package com.jjg.game.sampledata.bean;

import java.util.*;
import java.math.BigDecimal;



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

  /** 礼包购买金额 */
  protected BigDecimal cost;
  /** 奖励1 */
  protected Map<Integer,Long> getitem;
  /** 中奖图标数量 */
  protected int iconNum;
  /** 类型 */
  protected int type;
  /** 中奖权重值 */
  protected int weight;

  /** 返回礼包购买金额 */
  public BigDecimal getCost() {
    return cost;
  }

  /** 返回奖励1 */
  public Map<Integer,Long> getGetitem() {
    return getitem;
  }

  /** 返回中奖图标数量 */
  public int getIconNum() {
    return iconNum;
  }

  /** 返回类型 */
  public int getType() {
    return type;
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
