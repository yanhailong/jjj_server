package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName Freespin.xlsx
 * @sheetName freespin
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class FreespinCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "Freespin.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "freespin";

  /** 转盘金额下限 */
  protected int lowerlimit;
  /** 客户端展示多语言ID */
  protected int test;
  /** 转盘金额下限 */
  protected int upperlimit;

  /** 返回转盘金额下限 */
  public int getLowerlimit() {
    return lowerlimit;
  }

  /** 返回客户端展示多语言ID */
  public int getTest() {
    return test;
  }

  /** 返回转盘金额下限 */
  public int getUpperlimit() {
    return upperlimit;
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
