package com.jjg.game.hall.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName Item.xlsx
 * @sheetName Item
 * @author Auto.Generator
 * @date 2025年08月06日 20:26:26
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ItemCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "Item.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "Item";

  /** 使用道具后获得道具 */
  protected List<Integer> getItem;
  /** 使用道具后获得状态 */
  protected List<Integer> getStatus;
  /** 图标 */
  protected String icon;
  /** 名称 */
  protected String name;
  /** 最大堆叠数量 */
  protected int prop;
  /** 合成物品信息 */
  protected Map<Integer,Integer> syntheticProps;
  /** 描述 */
  protected String text;
  /** 类型 */
  protected int type;

  /** 返回使用道具后获得道具 */
  public List<Integer> getGetItem() {
    return getItem;
  }

  /** 返回使用道具后获得状态 */
  public List<Integer> getGetStatus() {
    return getStatus;
  }

  /** 返回图标 */
  public String getIcon() {
    return icon;
  }

  /** 返回名称 */
  public String getName() {
    return name;
  }

  /** 返回最大堆叠数量 */
  public int getProp() {
    return prop;
  }

  /** 返回合成物品信息 */
  public Map<Integer,Integer> getSyntheticProps() {
    return syntheticProps;
  }

  /** 返回描述 */
  public String getText() {
    return text;
  }

  /** 返回类型 */
  public int getType() {
    return type;
  }
}
