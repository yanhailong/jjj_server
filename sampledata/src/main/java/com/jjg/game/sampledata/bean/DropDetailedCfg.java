package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName dropDetailed.xlsx
 * @sheetName dropDetailed
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DropDetailedCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "dropDetailed.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "dropDetailed";

  /** 权重_道具ID */
  protected List<List<Integer>> detailedDropItem;

  /** 返回权重_道具ID */
  public List<List<Integer>> getDetailedDropItem() {
    return detailedDropItem;
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
