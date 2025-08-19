package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName BuildingFunction.xlsx
 * @sheetName BuildingFunction
 * @author Auto.Generator
 * @date 2025年08月19日 11:16:26
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BuildingFunctionCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "BuildingFunction.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "BuildingFunction";

  /** 建筑效果 */
  protected int buffid;
  /** 等级 */
  protected int buldlevel;
  /** 建筑多语言描述ID */
  protected int describe;
  /** 建筑多语言ID */
  protected int name;
  /** 下一级建筑ID */
  protected int nextlevelID;
  /** 基础产出/分钟 */
  protected List<Integer> output;
  /** 基础存储数量 */
  protected int savenum;
  /** 需要的空间大小 */
  protected int spaceSize;
  /** 建筑类型 */
  protected int typeID;
  /** 升下一级消耗道具ID */
  protected Map<Integer,Integer> uplevel_itemid;
  /** 升级耗时/秒 */
  protected int uptime;

  /** 返回建筑效果 */
  public int getBuffid() {
    return buffid;
  }

  /** 返回等级 */
  public int getBuldlevel() {
    return buldlevel;
  }

  /** 返回建筑多语言描述ID */
  public int getDescribe() {
    return describe;
  }

  /** 返回建筑多语言ID */
  public int getName() {
    return name;
  }

  /** 返回下一级建筑ID */
  public int getNextlevelID() {
    return nextlevelID;
  }

  /** 返回基础产出/分钟 */
  public List<Integer> getOutput() {
    return output;
  }

  /** 返回基础存储数量 */
  public int getSavenum() {
    return savenum;
  }

  /** 返回需要的空间大小 */
  public int getSpaceSize() {
    return spaceSize;
  }

  /** 返回建筑类型 */
  public int getTypeID() {
    return typeID;
  }

  /** 返回升下一级消耗道具ID */
  public Map<Integer,Integer> getUplevel_itemid() {
    return uplevel_itemid;
  }

  /** 返回升级耗时/秒 */
  public int getUptime() {
    return uptime;
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
