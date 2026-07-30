package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName Guide.xlsx
 * @sheetName Guide
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class GuideCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "Guide.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "Guide";

  /** 触发条件类型 */
  protected int Condition;
  /** 引导组编号 */
  protected int GuideGroupId;
  /** 引导列表ID */
  protected List<Integer> GuideIdList;
  /** 触发场景 */
  protected String PathName;
  /** 跳过奖励界面引导ID */
  protected List<Integer> SkipGuideId;
  /** 是否开启 */
  protected boolean isOpen;
  /** 条件参数 */
  protected int param;

  /** 返回触发条件类型 */
  public int getCondition() {
    return Condition;
  }

  /** 返回引导组编号 */
  public int getGuideGroupId() {
    return GuideGroupId;
  }

  /** 返回引导列表ID */
  public List<Integer> getGuideIdList() {
    return GuideIdList;
  }

  /** 返回触发场景 */
  public String getPathName() {
    return PathName;
  }

  /** 返回跳过奖励界面引导ID */
  public List<Integer> getSkipGuideId() {
    return SkipGuideId;
  }

  /** 返回是否开启 */
  public boolean getIsOpen() {
    return isOpen;
  }

  /** 返回条件参数 */
  public int getParam() {
    return param;
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
