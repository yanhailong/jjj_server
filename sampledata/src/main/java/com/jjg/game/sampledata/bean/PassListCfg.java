package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName PassList.xlsx
 * @sheetName PassList
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PassListCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "PassList.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "PassList";

  /** 是否跟随赛季进行 */
  protected boolean FollowsSeason;
  /** 通行证名称 */
  protected int PassName;
  /** 是否开启 */
  protected boolean isOpen;

  /** 返回是否跟随赛季进行 */
  public boolean getFollowsSeason() {
    return FollowsSeason;
  }

  /** 返回通行证名称 */
  public int getPassName() {
    return PassName;
  }

  /** 返回是否开启 */
  public boolean getIsOpen() {
    return isOpen;
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
