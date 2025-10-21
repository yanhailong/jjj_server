package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName SpecialMode.xlsx
 * @sheetName SpecialMode
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SpecialModeCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "SpecialMode.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "SpecialMode";

  /** 初始列数 */
  protected int cols;
  /** 游戏ID */
  protected int gameType;
  /** 此次玩法使用得滚轴组ID */
  protected int rollerMode;
  /** 初始行数 */
  protected int rows;
  /** 修改图案策略 */
  protected List<Integer> specialGirdID;
  /** 修改图案策略组，多个修改中按概率抽取1个修改 */
  protected Map<Integer,Integer> specialGroupGirdID;
  /** 执行次数 */
  protected int triggerCount;
  /** 游戏模式 */
  protected int type;

  /** 返回初始列数 */
  public int getCols() {
    return cols;
  }

  /** 返回游戏ID */
  public int getGameType() {
    return gameType;
  }

  /** 返回此次玩法使用得滚轴组ID */
  public int getRollerMode() {
    return rollerMode;
  }

  /** 返回初始行数 */
  public int getRows() {
    return rows;
  }

  /** 返回修改图案策略 */
  public List<Integer> getSpecialGirdID() {
    return specialGirdID;
  }

  /** 返回修改图案策略组，多个修改中按概率抽取1个修改 */
  public Map<Integer,Integer> getSpecialGroupGirdID() {
    return specialGroupGirdID;
  }

  /** 返回执行次数 */
  public int getTriggerCount() {
    return triggerCount;
  }

  /** 返回游戏模式 */
  public int getType() {
    return type;
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
