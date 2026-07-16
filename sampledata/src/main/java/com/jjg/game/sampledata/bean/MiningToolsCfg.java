package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName MiningTools.xlsx
 * @sheetName MiningTools
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class MiningToolsCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "MiningTools.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "MiningTools";

  /** 伤害 */
  protected int Damage;
  /** 道具ID */
  protected int itemid;

  /** 返回伤害 */
  public int getDamage() {
    return Damage;
  }

  /** 返回道具ID */
  public int getItemid() {
    return itemid;
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
