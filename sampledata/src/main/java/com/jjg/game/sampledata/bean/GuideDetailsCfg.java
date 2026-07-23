package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName GuideDetails.xlsx
 * @sheetName GuideDetails
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class GuideDetailsCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "GuideDetails.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "GuideDetails";

  /** 点击指引类型 */
  protected int ClickShowType;
  /** 是否强制引导 */
  protected boolean MustBeGuided;
  /** 语音文件 */
  protected String SoundFile;
  /** 多语言ID */
  protected int StrDes;
  /** 文本显示类型 */
  protected int TextShowType;

  /** 返回点击指引类型 */
  public int getClickShowType() {
    return ClickShowType;
  }

  /** 返回是否强制引导 */
  public boolean getMustBeGuided() {
    return MustBeGuided;
  }

  /** 返回语音文件 */
  public String getSoundFile() {
    return SoundFile;
  }

  /** 返回多语言ID */
  public int getStrDes() {
    return StrDes;
  }

  /** 返回文本显示类型 */
  public int getTextShowType() {
    return TextShowType;
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
