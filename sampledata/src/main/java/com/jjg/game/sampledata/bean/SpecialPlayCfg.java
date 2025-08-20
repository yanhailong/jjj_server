package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName SpecialPlay.xlsx
 * @sheetName SpecialPlay
 * @author Auto.Generator
 * @date 2025年08月20日 13:34:26
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SpecialPlayCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "SpecialPlay.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "SpecialPlay";

  /** 是否额外投注 */
  protected boolean extraBet;
  /** 游戏状态 */
  protected int gameStauts;
  /** 游戏ID */
  protected int gameType;
  /** 模式ID */
  protected int modelId;
  /** 玩法关键字 */
  protected int playType;
  /** 旋转标识 */
  protected int spinType;
  /** 值 */
  protected String value;

  /** 返回是否额外投注 */
  public boolean getExtraBet() {
    return extraBet;
  }

  /** 返回游戏状态 */
  public int getGameStauts() {
    return gameStauts;
  }

  /** 返回游戏ID */
  public int getGameType() {
    return gameType;
  }

  /** 返回模式ID */
  public int getModelId() {
    return modelId;
  }

  /** 返回玩法关键字 */
  public int getPlayType() {
    return playType;
  }

  /** 返回旋转标识 */
  public int getSpinType() {
    return spinType;
  }

  /** 返回值 */
  public String getValue() {
    return value;
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
