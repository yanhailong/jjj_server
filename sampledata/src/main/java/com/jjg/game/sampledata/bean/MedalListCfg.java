package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName MedalList.xlsx
 * @sheetName MedalList
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class MedalListCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "MedalList.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "MedalList";

  /** 是否开启 */
  protected boolean IsOpen;
  /** 勋章名称 */
  protected int LangId;
  /** 勋章图标 */
  protected String MedalIcon;
  /** 勋章品质 */
  protected int MedalType;
  /** 需求激活道具ID */
  protected int NeedItemId;
  /** 勋章描述 */
  protected int StrDes;

  /** 返回是否开启 */
  public boolean getIsOpen() {
    return IsOpen;
  }

  /** 返回勋章名称 */
  public int getLangId() {
    return LangId;
  }

  /** 返回勋章图标 */
  public String getMedalIcon() {
    return MedalIcon;
  }

  /** 返回勋章品质 */
  public int getMedalType() {
    return MedalType;
  }

  /** 返回需求激活道具ID */
  public int getNeedItemId() {
    return NeedItemId;
  }

  /** 返回勋章描述 */
  public int getStrDes() {
    return StrDes;
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
