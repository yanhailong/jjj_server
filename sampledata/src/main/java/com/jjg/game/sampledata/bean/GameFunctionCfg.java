package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName GameFunction.xlsx
 * @sheetName GameFunction
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class GameFunctionCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "GameFunction.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "GameFunction";

  /** 游戏中左侧列表按钮 */
  protected int butList;
  /** 可看见的解锁条件 */
  protected String condition;
  /** 显示区域类型 */
  protected int displayArea;
  /** 对应按钮节点 */
  protected String functionNodeName;
  /** 功能类型 */
  protected int functionType;
  /** 开启状态 */
  protected boolean isOpen;
  /** 显示位置 */
  protected List<Integer> position;
  /** 显示顺序 */
  protected int serialNumber;
  /** 提示文本ID */
  protected int tips;

  /** 返回游戏中左侧列表按钮 */
  public int getButList() {
    return butList;
  }

  /** 返回可看见的解锁条件 */
  public String getCondition() {
    return condition;
  }

  /** 返回显示区域类型 */
  public int getDisplayArea() {
    return displayArea;
  }

  /** 返回对应按钮节点 */
  public String getFunctionNodeName() {
    return functionNodeName;
  }

  /** 返回功能类型 */
  public int getFunctionType() {
    return functionType;
  }

  /** 返回开启状态 */
  public boolean getIsOpen() {
    return isOpen;
  }

  /** 返回显示位置 */
  public List<Integer> getPosition() {
    return position;
  }

  /** 返回显示顺序 */
  public int getSerialNumber() {
    return serialNumber;
  }

  /** 返回提示文本ID */
  public int getTips() {
    return tips;
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
