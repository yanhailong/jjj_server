package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName BuildingGain.xlsx
 * @sheetName BuildingGain
 * @author Auto.Generator
 * @date 2025年08月20日 13:34:26
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BuildingGainCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "BuildingGain.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "BuildingGain";

  /** 增加值(万分比) */
  protected int addvalue;
  /** 类型 */
  protected int bufftype;
  /** 等级 */
  protected int level;

  /** 返回增加值(万分比) */
  public int getAddvalue() {
    return addvalue;
  }

  /** 返回类型 */
  public int getBufftype() {
    return bufftype;
  }

  /** 返回等级 */
  public int getLevel() {
    return level;
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
