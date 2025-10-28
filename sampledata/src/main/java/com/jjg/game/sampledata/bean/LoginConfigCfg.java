package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName loginConfig.xlsx
 * @sheetName loginConfig
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class LoginConfigCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "loginConfig.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "loginConfig";

  /** 绑定奖励内容 */
  protected Map<Integer,Long> awardItem;
  /** 奖励类型 */
  protected int type;

  /** 返回绑定奖励内容 */
  public Map<Integer,Long> getAwardItem() {
    return awardItem;
  }

  /** 返回奖励类型 */
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
