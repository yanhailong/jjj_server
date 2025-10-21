package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName gameList.xlsx
 * @sheetName GameList
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class GameListCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "gameList.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "GameList";

  /** 图标类型 */
  protected int iconType;
  /** 名称 */
  protected String name;
  /** 状态  0.开启  1.维护  2.关闭 */
  protected int status;

  /** 返回图标类型 */
  public int getIconType() {
    return iconType;
  }

  /** 返回名称 */
  public String getName() {
    return name;
  }

  /** 返回状态  0.开启  1.维护  2.关闭 */
  public int getStatus() {
    return status;
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
