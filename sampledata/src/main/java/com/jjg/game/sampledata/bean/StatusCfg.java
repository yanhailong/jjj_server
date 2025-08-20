package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName status.xlsx
 * @sheetName status
 * @author Auto.Generator
 * @date 2025年08月20日 13:34:26
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class StatusCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "status.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "status";

  /** 图标 */
  protected String icon;
  /** 名称 */
  protected String name;
  /** 最大堆叠数量 */
  protected int prop;
  /** 参数 */
  protected int staterate;
  /** 描述 */
  protected String text;
  /** 持续时间 */
  protected int time;
  /** 时间类型 */
  protected int timeType;
  /** 类型 */
  protected int type;

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

  /** 返回参数 */
  public int getStaterate() {
    return staterate;
  }

  /** 返回描述 */
  public String getText() {
    return text;
  }

  /** 返回持续时间 */
  public int getTime() {
    return time;
  }

  /** 返回时间类型 */
  public int getTimeType() {
    return timeType;
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
