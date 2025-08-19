package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName BuildingFloor.xlsx
 * @sheetName BuildingFloor
 * @author Auto.Generator
 * @date 2025年08月19日 15:29:42
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BuildingFloorCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "BuildingFloor.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "BuildingFloor";

  /** 楼层建筑组成 */
  protected List<Integer> Architectural;
  /** 赌场ID */
  protected int area;
  /** 楼层背景资源名称 */
  protected String background;
  /** 清扫时间(秒) */
  protected int cleartime;
  /** 楼层ID */
  protected int type;
  /** 楼层解锁条件 */
  protected List<Integer> unlock;

  /** 返回楼层建筑组成 */
  public List<Integer> getArchitectural() {
    return Architectural;
  }

  /** 返回赌场ID */
  public int getArea() {
    return area;
  }

  /** 返回楼层背景资源名称 */
  public String getBackground() {
    return background;
  }

  /** 返回清扫时间(秒) */
  public int getCleartime() {
    return cleartime;
  }

  /** 返回楼层ID */
  public int getType() {
    return type;
  }

  /** 返回楼层解锁条件 */
  public List<Integer> getUnlock() {
    return unlock;
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
