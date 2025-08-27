package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName ActivityConfig.xlsx
 * @sheetName ActivityConfig
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ActivityConfigCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "ActivityConfig.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "ActivityConfig";

  /** 跑马灯通知 */
  protected boolean marquee;
  /** 活动名称 */
  protected int name;
  /** 解锁条件 */
  protected int needlevel;
  /** 是否开启 */
  protected boolean open;
  /** 活动开启方式 */
  protected int open_type;
  /** 活动底图名称 */
  protected String picname;
  /** 子类型 */
  protected String sub_type;
  /** 结束时间 */
  protected String time_end;
  /** 开始时间 */
  protected String time_start;
  /** 类型ID */
  protected int type;

  /** 返回跑马灯通知 */
  public boolean getMarquee() {
    return marquee;
  }

  /** 返回活动名称 */
  public int getName() {
    return name;
  }

  /** 返回解锁条件 */
  public int getNeedlevel() {
    return needlevel;
  }

  /** 返回是否开启 */
  public boolean getOpen() {
    return open;
  }

  /** 返回活动开启方式 */
  public int getOpen_type() {
    return open_type;
  }

  /** 返回活动底图名称 */
  public String getPicname() {
    return picname;
  }

  /** 返回子类型 */
  public String getSub_type() {
    return sub_type;
  }

  /** 返回结束时间 */
  public String getTime_end() {
    return time_end;
  }

  /** 返回开始时间 */
  public String getTime_start() {
    return time_start;
  }

  /** 返回类型ID */
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
