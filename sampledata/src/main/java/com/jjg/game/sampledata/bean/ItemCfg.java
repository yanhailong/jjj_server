package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName Item.xlsx
 * @sheetName Item
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ItemCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "Item.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "Item";

  /** 自动激活的图鉴ID */
  protected int album;
  /** 激活的装扮 */
  protected int avatarID;
  /** 是否允许在背包内展示 */
  protected int displayOrNot;
  /** 掉落ID */
  protected int dropId;
  /** 使用道具后获得道具 */
  protected Map<Integer,Long> getItem;
  /** 图标资源名 */
  protected String icon;
  /** 名称多语言ID */
  protected int name;
  /** 最大堆叠数量 */
  protected int prop;
  /** 道具品质 */
  protected int quality;
  /** 描述多语言ID */
  protected int text;
  /** 类型 */
  protected int type;

  /** 返回自动激活的图鉴ID */
  public int getAlbum() {
    return album;
  }

  /** 返回激活的装扮 */
  public int getAvatarID() {
    return avatarID;
  }

  /** 返回是否允许在背包内展示 */
  public int getDisplayOrNot() {
    return displayOrNot;
  }

  /** 返回掉落ID */
  public int getDropId() {
    return dropId;
  }

  /** 返回使用道具后获得道具 */
  public Map<Integer,Long> getGetItem() {
    return getItem;
  }

  /** 返回图标资源名 */
  public String getIcon() {
    return icon;
  }

  /** 返回名称多语言ID */
  public int getName() {
    return name;
  }

  /** 返回最大堆叠数量 */
  public int getProp() {
    return prop;
  }

  /** 返回道具品质 */
  public int getQuality() {
    return quality;
  }

  /** 返回描述多语言ID */
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
