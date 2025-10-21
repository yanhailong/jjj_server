package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName BaseRoller.xlsx
 * @sheetName BaseRoller
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BaseRollerCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "BaseRoller.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "BaseRoller";

  /** 滚轴数量和范围 */
  protected List<Integer> axleCountScope;
  /** 客户端展示用的普通模式滚轴 */
  protected List<Integer> clientRollerAxleCountScope;
  /** 元素列表 */
  protected List<Integer> clientRollerElements;
  /** 序号 */
  protected int column;
  /** 滚轴图案 */
  protected List<Integer> elements;
  /** 游戏ID */
  protected int gameType;
  /** 初始化格子 */
  protected List<Integer> initGrid;
  /** 滚轴组ID */
  protected int rollerGroup;

  /** 返回滚轴数量和范围 */
  public List<Integer> getAxleCountScope() {
    return axleCountScope;
  }

  /** 返回客户端展示用的普通模式滚轴 */
  public List<Integer> getClientRollerAxleCountScope() {
    return clientRollerAxleCountScope;
  }

  /** 返回元素列表 */
  public List<Integer> getClientRollerElements() {
    return clientRollerElements;
  }

  /** 返回序号 */
  public int getColumn() {
    return column;
  }

  /** 返回滚轴图案 */
  public List<Integer> getElements() {
    return elements;
  }

  /** 返回游戏ID */
  public int getGameType() {
    return gameType;
  }

  /** 返回初始化格子 */
  public List<Integer> getInitGrid() {
    return initGrid;
  }

  /** 返回滚轴组ID */
  public int getRollerGroup() {
    return rollerGroup;
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
