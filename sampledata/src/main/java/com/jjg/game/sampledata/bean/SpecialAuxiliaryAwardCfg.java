package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName SpecialAuxiliaryAward.xlsx
 * @sheetName SpecialAuxiliaryAward
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SpecialAuxiliaryAwardCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "SpecialAuxiliaryAward.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "SpecialAuxiliaryAward";

  /** 奖励类型A */
  protected List<List<Integer>> awardTypeA;
  /** 奖励类型B */
  protected String awardTypeB;
  /** 奖励类型C */
  protected List<String> awardTypeC;
  /** 游戏类型 */
  protected int gameType;
  /** 类型 */
  protected int type;

  /** 返回奖励类型A */
  public List<List<Integer>> getAwardTypeA() {
    return awardTypeA;
  }

  /** 返回奖励类型B */
  public String getAwardTypeB() {
    return awardTypeB;
  }

  /** 返回奖励类型C */
  public List<String> getAwardTypeC() {
    return awardTypeC;
  }

  /** 返回游戏类型 */
  public int getGameType() {
    return gameType;
  }

  /** 返回类型 */
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
