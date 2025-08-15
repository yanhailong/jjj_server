package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName AuxiliaryAward.xlsx
 * @sheetName AuxiliaryAward
 * @author Auto.Generator
 * @date 2025年08月15日 17:50:22
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class AuxiliaryAwardCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "AuxiliaryAward.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "AuxiliaryAward";

  /** 小游戏奖励ID */
  protected int auxiliaryID;
  /** 奖励数值 */
  protected int auxiliaryMulti;
  /** 小游戏奖励类型 */
  protected int auxiliaryType;
  /** 额外中奖区域 */
  protected Map<Integer,List<Integer>> extraWinArea;
  /** 游戏ID */
  protected int gameID;
  /** 奖励位置个数 */
  protected Map<Integer,Integer> posNum;
  /** 随机位置 */
  protected List<List<Integer>> posRandom;

  /** 返回小游戏奖励ID */
  public int getAuxiliaryID() {
    return auxiliaryID;
  }

  /** 返回奖励数值 */
  public int getAuxiliaryMulti() {
    return auxiliaryMulti;
  }

  /** 返回小游戏奖励类型 */
  public int getAuxiliaryType() {
    return auxiliaryType;
  }

  /** 返回额外中奖区域 */
  public Map<Integer,List<Integer>> getExtraWinArea() {
    return extraWinArea;
  }

  /** 返回游戏ID */
  public int getGameID() {
    return gameID;
  }

  /** 返回奖励位置个数 */
  public Map<Integer,Integer> getPosNum() {
    return posNum;
  }

  /** 返回随机位置 */
  public List<List<Integer>> getPosRandom() {
    return posRandom;
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
