package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName dropGroup.xlsx
 * @sheetName dropGroup
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DropGroupCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "dropGroup.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "dropGroup";

  /** 每天掉落次数 */
  protected int dropCount;
  /** 子包组 */
  protected List<List<Integer>> dropDetailedID;
  /** 掉落主ID */
  protected int trunkID;

  /** 返回每天掉落次数 */
  public int getDropCount() {
    return dropCount;
  }

  /** 返回子包组 */
  public List<List<Integer>> getDropDetailedID() {
    return dropDetailedID;
  }

  /** 返回掉落主ID */
  public int getTrunkID() {
    return trunkID;
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
