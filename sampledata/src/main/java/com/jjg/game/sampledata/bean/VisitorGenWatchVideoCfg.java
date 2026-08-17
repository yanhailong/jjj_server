package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName VisitorGenWatchVideo.xlsx
 * @sheetName VisitorGenWatchVideo
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class VisitorGenWatchVideoCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "VisitorGenWatchVideo.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "VisitorGenWatchVideo";

  /** 单日观看次数上限 */
  protected int DailyViewLimit;
  /** 观看CD时间（min） */
  protected int ViewCD;
  /** 单个游客数量 */
  protected int VisitorCount;
  /** 游客道具ID */
  protected int VisitorID;
  /** 客户端资源 */
  protected String icon;

  /** 返回单日观看次数上限 */
  public int getDailyViewLimit() {
    return DailyViewLimit;
  }

  /** 返回观看CD时间（min） */
  public int getViewCD() {
    return ViewCD;
  }

  /** 返回单个游客数量 */
  public int getVisitorCount() {
    return VisitorCount;
  }

  /** 返回游客道具ID */
  public int getVisitorID() {
    return VisitorID;
  }

  /** 返回客户端资源 */
  public String getIcon() {
    return icon;
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
