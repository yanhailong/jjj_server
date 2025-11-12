package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName popUpConfig.xlsx
 * @sheetName popUpConfig
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PopUpConfigCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "popUpConfig.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "popUpConfig";

  /** 每次登入弹出 */
  protected boolean boolLogin;
  /** 回大厅弹出 */
  protected boolean boolReturnHall;
  /** 每天弹窗次数 */
  protected int count;
  /** 弹窗结束时间 */
  protected String endTime;
  /** 活动或功能界面名称 */
  protected List<List<String>> functionUiName;
  /** 宣传弹窗界面名称 */
  protected List<String> imgName;
  /** 是否开启 */
  protected boolean isOpen;
  /** 打开界面需要导入的脚本 */
  protected List<String> requires;
  /** 弹窗顺序 */
  protected int serialNumber;
  /** 弹窗开始时间 */
  protected String strarTime;
  /** 弹窗类型值 */
  protected List<List<Integer>> typeValue;

  /** 返回每次登入弹出 */
  public boolean getBoolLogin() {
    return boolLogin;
  }

  /** 返回回大厅弹出 */
  public boolean getBoolReturnHall() {
    return boolReturnHall;
  }

  /** 返回每天弹窗次数 */
  public int getCount() {
    return count;
  }

  /** 返回弹窗结束时间 */
  public String getEndTime() {
    return endTime;
  }

  /** 返回活动或功能界面名称 */
  public List<List<String>> getFunctionUiName() {
    return functionUiName;
  }

  /** 返回宣传弹窗界面名称 */
  public List<String> getImgName() {
    return imgName;
  }

  /** 返回是否开启 */
  public boolean getIsOpen() {
    return isOpen;
  }

  /** 返回打开界面需要导入的脚本 */
  public List<String> getRequires() {
    return requires;
  }

  /** 返回弹窗顺序 */
  public int getSerialNumber() {
    return serialNumber;
  }

  /** 返回弹窗开始时间 */
  public String getStrarTime() {
    return strarTime;
  }

  /** 返回弹窗类型值 */
  public List<List<Integer>> getTypeValue() {
    return typeValue;
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
