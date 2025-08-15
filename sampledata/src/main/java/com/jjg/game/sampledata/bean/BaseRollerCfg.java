package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName BaseRoller.xlsx
 * @sheetName BaseRoller
 * @author Auto.Generator
 * @date 2025年08月15日 18:30:10
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BaseRollerCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "BaseRoller.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "BaseRoller";

  /** 滚轴数量和范围 */
  protected Map<Integer,List<Integer>> axleCountScope;
  /** 列数 */
  protected int column;
  /** 元素列表 */
  protected List<Integer> elements;
  /** 游戏ID */
  protected int gameType;

  /** 返回滚轴数量和范围 */
  public Map<Integer,List<Integer>> getAxleCountScope() {
    return axleCountScope;
  }

  /** 返回列数 */
  public int getColumn() {
    return column;
  }

  /** 返回元素列表 */
  public List<Integer> getElements() {
    return elements;
  }

  /** 返回游戏ID */
  public int getGameType() {
    return gameType;
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
