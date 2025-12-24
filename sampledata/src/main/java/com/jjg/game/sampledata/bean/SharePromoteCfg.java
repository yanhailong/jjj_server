package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName SharePromote.xlsx
 * @sheetName SharePromote
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SharePromoteCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "SharePromote.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "SharePromote";

  /** 名次 */
  protected List<Integer> Ranking;
  /** 有效下注的收益比例 */
  protected int betproportion;
  /** 激活条件 */
  protected int condition;
  /** 奖励 */
  protected Map<Integer,Long> getitem;
  /** 充值收益比例 */
  protected int proportion;
  /** 类型 */
  protected int type;

  /** 返回名次 */
  public List<Integer> getRanking() {
    return Ranking;
  }

  /** 返回有效下注的收益比例 */
  public int getBetproportion() {
    return betproportion;
  }

  /** 返回激活条件 */
  public int getCondition() {
    return condition;
  }

  /** 返回奖励 */
  public Map<Integer,Long> getGetitem() {
    return getitem;
  }

  /** 返回充值收益比例 */
  public int getProportion() {
    return proportion;
  }

  /** 返回类型 */
  public int getType() {
    return type;
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
