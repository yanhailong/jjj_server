package com.jjg.game.hall.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName Item.xlsx
 * @sheetName Item
 * @author Auto.Generator
 * @date 2025年08月08日 13:44:57
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ItemCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "Item.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "Item";

  /** 使用道具后获得道具 */
  protected List<List<Integer>> getItem;
  /** 图标 */
  protected String icon;
  /** 名称 */
  protected int name;
  /** 最大堆叠数量 */
  protected int prop;
  /** 描述 */
  protected int text;
  /** 类型 */
  protected int type;

  /** 返回使用道具后获得道具 */
  public List<List<Integer>> getGetItem() {
    return getItem;
  }

  /** 返回图标 */
  public String getIcon() {
    return icon;
  }

  /** 返回名称 */
  public int getName() {
    return name;
  }

  /** 返回最大堆叠数量 */
  public int getProp() {
    return prop;
  }

  /** 返回描述 */
  public int getText() {
    return text;
  }

  /** 返回类型 */
  public int getType() {
    return type;
  }
}
