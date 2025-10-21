package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName dropConfig.xlsx
 * @sheetName dropConfig
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DropConfigCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "dropConfig.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "dropConfig";

  /** 掉落条件 */
  protected List<String> dropCondition;
  /** 道具掉落包ID */
  protected List<Integer> dropId;
  /** 进度触发类型 */
  protected Map<Integer,Integer> triggerType;

  /** 返回掉落条件 */
  public List<String> getDropCondition() {
    return dropCondition;
  }

  /** 返回道具掉落包ID */
  public List<Integer> getDropId() {
    return dropId;
  }

  /** 返回进度触发类型 */
  public Map<Integer,Integer> getTriggerType() {
    return triggerType;
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
