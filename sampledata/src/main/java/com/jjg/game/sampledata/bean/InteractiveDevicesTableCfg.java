package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName InteractiveDevicesTable.xlsx
 * @sheetName InteractiveDevicesTable
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class InteractiveDevicesTableCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "InteractiveDevicesTable.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "InteractiveDevicesTable";

  /** 建筑等级 */
  protected int BuildingLevel;
  /** 设备数量 */
  protected int DeviceCount;
  /** 楼层 */
  protected int Floor;
  /** 交互耗时(秒) */
  protected int InteractionTime;
  /** 等待区容量(人) */
  protected int QueueLimit;
  /** 接待上限(人) */
  protected int SeatingCapacity;

  /** 返回建筑等级 */
  public int getBuildingLevel() {
    return BuildingLevel;
  }

  /** 返回设备数量 */
  public int getDeviceCount() {
    return DeviceCount;
  }

  /** 返回楼层 */
  public int getFloor() {
    return Floor;
  }

  /** 返回交互耗时(秒) */
  public int getInteractionTime() {
    return InteractionTime;
  }

  /** 返回等待区容量(人) */
  public int getQueueLimit() {
    return QueueLimit;
  }

  /** 返回接待上限(人) */
  public int getSeatingCapacity() {
    return SeatingCapacity;
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
