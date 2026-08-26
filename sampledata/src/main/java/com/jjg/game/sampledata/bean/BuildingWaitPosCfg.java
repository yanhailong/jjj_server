package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName BuildingWaitPos.xlsx
 * @sheetName BuildingWaitPos
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BuildingWaitPosCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "BuildingWaitPos.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "BuildingWaitPos";

  /** 建筑ID */
  protected int BuildingID;

  /** 返回建筑ID */
  public int getBuildingID() {
    return BuildingID;
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
