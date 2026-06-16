package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName PoolList.xlsx
 * @sheetName PoolList
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PoolListCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "PoolList.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "PoolList";

  /** 掉落物品 */
  protected int DropItem;
  /** 开启状态 */
  protected boolean open;
  /** 结束时间 */
  protected String time_end;
  /** 开启时间 */
  protected String time_start;
  /** 卡池类型 */
  protected int type;

  /** 返回掉落物品 */
  public int getDropItem() {
    return DropItem;
  }

  /** 返回开启状态 */
  public boolean getOpen() {
    return open;
  }

  /** 返回结束时间 */
  public String getTime_end() {
    return time_end;
  }

  /** 返回开启时间 */
  public String getTime_start() {
    return time_start;
  }

  /** 返回卡池类型 */
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
