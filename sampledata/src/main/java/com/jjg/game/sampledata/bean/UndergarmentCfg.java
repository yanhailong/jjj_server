package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName undergarment.xlsx
 * @sheetName undergarment
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class UndergarmentCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "undergarment.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "undergarment";

  /** apple信息 */
  protected String appleAppid;
  /** apple信息 */
  protected String appleBundleId;
  /** facebook信息 */
  protected String faceBookAppSecret;
  /** facebook信息 */
  protected String facebookAppid;
  /** 允许的游戏 */
  protected List<Integer> gamelist;
  /** google信息 */
  protected String googleClientID;

  /** 返回apple信息 */
  public String getAppleAppid() {
    return appleAppid;
  }

  /** 返回apple信息 */
  public String getAppleBundleId() {
    return appleBundleId;
  }

  /** 返回facebook信息 */
  public String getFaceBookAppSecret() {
    return faceBookAppSecret;
  }

  /** 返回facebook信息 */
  public String getFacebookAppid() {
    return facebookAppid;
  }

  /** 返回允许的游戏 */
  public List<Integer> getGamelist() {
    return gamelist;
  }

  /** 返回google信息 */
  public String getGoogleClientID() {
    return googleClientID;
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
