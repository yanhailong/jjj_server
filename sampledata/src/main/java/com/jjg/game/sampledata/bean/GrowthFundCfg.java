package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName GrowthFund.xlsx
 * @sheetName GrowthFund
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class GrowthFundCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "GrowthFund.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "GrowthFund";

  /** 道具奖励 */
  protected Map<Integer,Long> getItem;
  /** 达成条件(角色等级) */
  protected int level;
  /** 类型 1免费 2付费 */
  protected int type;

  /** 返回道具奖励 */
  public Map<Integer,Long> getGetItem() {
    return getItem;
  }

  /** 返回达成条件(角色等级) */
  public int getLevel() {
    return level;
  }

  /** 返回类型 1免费 2付费 */
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
