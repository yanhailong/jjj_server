package com.jjg.game.sampledata.bean;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName UpcomingMobileGame.xlsx
 * @sheetName UpcomingMobileGame
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class UpcomingMobileGameCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "UpcomingMobileGame.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "UpcomingMobileGame";

  /**
   * 是否显示
   */
  protected boolean display;
  /** 游戏名称多语言 */
  protected int name;
  /** 游戏图片资源名 */
  protected String picture;
  /** 描述多语言ID */
  protected int text;

  /** 返回是否显示 */
  public boolean getDisplay() {
    return display;
  }

  /** 返回游戏名称多语言 */
  public int getName() {
    return name;
  }

  /** 返回游戏图片资源名 */
  public String getPicture() {
    return picture;
  }

  /** 返回描述多语言ID */
  public int getText() {
    return text;
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
