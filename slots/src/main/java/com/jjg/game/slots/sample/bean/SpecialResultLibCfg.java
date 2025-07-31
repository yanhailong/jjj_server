package com.jjg.game.slots.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName SpecialResultLib.xlsx
 * @sheetName SpecialResultLib
 * @author Auto.Generator
 * @date 2025年07月30日 10:16:30
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SpecialResultLibCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "SpecialResultLib.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "SpecialResultLib";

  /** 进入条件 */
  protected int enterLimitMax;
  /** 进入条件 */
  protected int enterLimitMin;
  /** 是否额外投注 */
  protected boolean extraBet;
  /** 游戏ID */
  protected int gameType;
  /** 线注倍数 */
  protected int lineTimes;
  /** 滚轴模式ID */
  protected int modelId;
  /** 倍数区间及权重 */
  protected Map<Integer,List<String>> sectionProp;
  /** 换皮游戏ID */
  protected int targetGameType;
  /** 类型权重 */
  protected Map<Integer,Integer> typeProp;

  /** 返回进入条件 */
  public int getEnterLimitMax() {
    return enterLimitMax;
  }

  /** 返回进入条件 */
  public int getEnterLimitMin() {
    return enterLimitMin;
  }

  /** 返回是否额外投注 */
  public boolean getExtraBet() {
    return extraBet;
  }

  /** 返回游戏ID */
  public int getGameType() {
    return gameType;
  }

  /** 返回线注倍数 */
  public int getLineTimes() {
    return lineTimes;
  }

  /** 返回滚轴模式ID */
  public int getModelId() {
    return modelId;
  }

  /** 返回倍数区间及权重 */
  public Map<Integer,List<String>> getSectionProp() {
    return sectionProp;
  }

  /** 返回换皮游戏ID */
  public int getTargetGameType() {
    return targetGameType;
  }

  /** 返回类型权重 */
  public Map<Integer,Integer> getTypeProp() {
    return typeProp;
  }
}
