package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName PoolResultLib.xlsx
 * @sheetName PoolResultLib
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PoolResultLibCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "PoolResultLib.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "PoolResultLib";

  /** 进入条件上限 */
  protected int enterLimitMax;
  /** 进入条件下线 */
  protected int enterLimitMin;
  /** 游戏ID */
  protected int gameType;
  /** 调控序列ID */
  protected int modelId;
  /** 类型权重 */
  protected Map<Integer,Integer> typeProp;

  /** 返回进入条件上限 */
  public int getEnterLimitMax() {
    return enterLimitMax;
  }

  /** 返回进入条件下线 */
  public int getEnterLimitMin() {
    return enterLimitMin;
  }

  /** 返回游戏ID */
  public int getGameType() {
    return gameType;
  }

  /** 返回调控序列ID */
  public int getModelId() {
    return modelId;
  }

  /** 返回类型权重 */
  public Map<Integer,Integer> getTypeProp() {
    return typeProp;
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
