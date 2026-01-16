package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName iponeAreacodeConfig.xlsx
 * @sheetName iponeAreacodeConfig
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class IponeAreacodeConfigCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "iponeAreacodeConfig.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "iponeAreacodeConfig";

  /** 需要屏蔽的号段 */
  protected List<String> blockedNumber;
  /** 名称 */
  protected int langId;
  /** 区号 */
  protected int type;

  /** 返回需要屏蔽的号段 */
  public List<String> getBlockedNumber() {
    return blockedNumber;
  }

  /** 返回名称 */
  public int getLangId() {
    return langId;
  }

  /** 返回区号 */
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
