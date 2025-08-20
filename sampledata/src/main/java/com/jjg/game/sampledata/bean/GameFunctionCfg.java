package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName GameFunction.xlsx
 * @sheetName GameFunction
 * @author Auto.Generator
 * @date 2025年08月20日 13:34:26
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class GameFunctionCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "GameFunction.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "GameFunction";

  /** 解锁条件-vip */
  protected List<Integer> vipLevel;

  /** 返回解锁条件-vip */
  public List<Integer> getVipLevel() {
    return vipLevel;
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
