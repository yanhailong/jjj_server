package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName MedalList.xlsx
 * @sheetName MedalList
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class MedalListCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "MedalList.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "MedalList";

  /** 需求道具 */
  protected int ItemId;
  /** 勋章图标资源 */
  protected String PicRes;
  /** 场景ID */
  protected int RegionID;

  /** 返回需求道具 */
  public int getItemId() {
    return ItemId;
  }

  /** 返回勋章图标资源 */
  public String getPicRes() {
    return PicRes;
  }

  /** 返回场景ID */
  public int getRegionID() {
    return RegionID;
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
