package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName BaseElement.xlsx
 * @sheetName BaseElement
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BaseElementCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "BaseElement.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "BaseElement";

  /** 元素ID */
  protected int elementId;
  /** 游戏ID */
  protected int gameId;
  /** 图标上显示多语言ID */
  protected int languageID;
  /** 消除后变化元素ID */
  protected int postChangeElementId;
  /** 向上占据格子 */
  protected int space;
  /** 类型 */
  protected int type;

  /** 返回元素ID */
  public int getElementId() {
    return elementId;
  }

  /** 返回游戏ID */
  public int getGameId() {
    return gameId;
  }

  /** 返回图标上显示多语言ID */
  public int getLanguageID() {
    return languageID;
  }

  /** 返回消除后变化元素ID */
  public int getPostChangeElementId() {
    return postChangeElementId;
  }

  /** 返回向上占据格子 */
  public int getSpace() {
    return space;
  }

  /** 返回类型 */
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
