package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName dropGroup.xlsx
 * @sheetName dropGroup
 * @author Auto.Generator
 * @date 2025年08月20日 13:34:26
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DropGroupCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "dropGroup.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "dropGroup";

  /** 子包组 */
  protected Map<Integer,Long> dropDetailedID;
  /** 掉落主ID */
  protected int trunkID;

  /** 返回子包组 */
  public Map<Integer,Long> getDropDetailedID() {
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
