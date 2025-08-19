package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName BaseElement.xlsx
 * @sheetName BaseElement
 * @author Auto.Generator
 * @date 2025年08月19日 11:16:26
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
