package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName PiggyBank.xlsx
 * @sheetName PiggyBank
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PiggyBankCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "PiggyBank.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "PiggyBank";

  /** 奖励1 */
  protected Map<Integer,Integer> getitem;
  /** 罐子类型 */
  protected int name;
  /** 购买金额 */
  protected int pay;
  /** 重置时间(天) */
  protected int resetime;
  /** 万分比 */
  protected int weight;

  /** 返回奖励1 */
  public Map<Integer,Integer> getGetitem() {
    return getitem;
  }

  /** 返回罐子类型 */
  public int getName() {
    return name;
  }

  /** 返回购买金额 */
  public int getPay() {
    return pay;
  }

  /** 返回重置时间(天) */
  public int getResetime() {
    return resetime;
  }

  /** 返回万分比 */
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
