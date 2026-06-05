package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName dropType.xlsx
 * @sheetName dropType
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DropTypeCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "dropType.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "dropType";

  /** 掉落方式 */
  protected List<Integer> DropType;

  /** 返回掉落方式 */
  public List<Integer> getDropType() {
    return DropType;
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
