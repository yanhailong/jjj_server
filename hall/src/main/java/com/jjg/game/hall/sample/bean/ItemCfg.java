package com.jjg.game.hall.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName Item.xlsx
 * @sheetName Item
 * @author Auto.Generator
 * @date 2025年08月15日 15:43:07
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ItemCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "Item.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "Item";

  /** 使用道具后获得道具 */
  protected Map<Integer,Long> getItem;
  /** 图标 */
  protected String icon;
  /** 名称 */
  protected int name;
  /** 最大堆叠数量 */
  protected int prop;
  /** 道具品质ID */
  protected int quality;
  /** 描述 */
  protected int text;
  /** 类型 */
  protected int type;

  /** 返回使用道具后获得道具 */
  public Map<Integer,Long> getGetItem() {
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

  /** 返回道具品质ID */
  public int getQuality() {
    return quality;
  }

  /** 返回描述 */
  public int getText() {
    return text;
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
