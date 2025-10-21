package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName RoomExpend.xlsx
 * @sheetName RoomExpend
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class RoomExpendCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "RoomExpend.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "RoomExpend";

  /** 持续时间(分钟） */
  protected int durationTime;
  /** 类型 */
  protected int durationtype;
  /** 消耗道具 */
  protected List<Integer> requiredItem;
  /** 消耗货币 */
  protected List<Integer> requiredMoney;

  /** 返回持续时间(分钟） */
  public int getDurationTime() {
    return durationTime;
  }

  /** 返回类型 */
  public int getDurationtype() {
    return durationtype;
  }

  /** 返回消耗道具 */
  public List<Integer> getRequiredItem() {
    return requiredItem;
  }

  /** 返回消耗货币 */
  public List<Integer> getRequiredMoney() {
    return requiredMoney;
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
