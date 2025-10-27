package com.jjg.game.sampledata.bean;

import javax.annotation.processing.Generated;
import java.math.BigDecimal;
import java.util.List;
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

  /** 值3 */
  protected List<BigDecimal> bigDecimalParam;
  /** 解锁条件 */
  protected String condition;
  /** 掉落id */
  protected int dropConfigId;
  /** 活动持续时间(天) */
  protected int duration;
  /** 跑马灯通知 */
  protected int marquee;
  /** 活动名称 */
  protected int name;
  /** 是否开启 */
  protected boolean open;
  /** 活动开启方式(1开服、2限时) */
  protected int open_type;
  /** 结束时间 */
  protected String time_end;
  /** 开始时间 */
  protected String time_start;
  /** 类型ID */
  protected int type;
  /** 值1 */
  protected List<Integer> value;
  /** 值2 */
  protected List<Long> valueParam;

  /** 返回值3 */
  public List<BigDecimal> getBigDecimalParam() {
    return bigDecimalParam;
  }

  /** 返回解锁条件 */
  public String getCondition() {
    return condition;
  }

  /** 返回掉落id */
  public int getDropConfigId() {
    return dropConfigId;
  }

  /** 返回活动持续时间(天) */
  public int getDuration() {
    return duration;
  }

  /** 返回跑马灯通知 */
  public int getMarquee() {
    return marquee;
  }

  /** 返回活动名称 */
  public int getName() {
    return name;
  }

  /** 返回是否开启 */
  public boolean getOpen() {
    return open;
  }

  /** 返回活动开启方式(1开服、2限时) */
  public int getOpen_type() {
    return open_type;
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

  /** 返回值1 */
  public List<Integer> getValue() {
    return value;
  }

  /** 返回值2 */
  public List<Long> getValueParam() {
    return valueParam;
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
