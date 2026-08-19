package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName VisitorBonds.xlsx
 * @sheetName VisitorBonds
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class VisitorBondsCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "VisitorBonds.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "VisitorBonds";

  /** 成员组成 */
  protected List<Integer> Members;
  /** 多语言 */
  protected int NameId;
  /** 属性提升 */
  protected int StatBoost;
  /** 游戏ID */
  protected int gameType;
  /** 多语言ID */
  protected int languageID;
  /** 进入特殊模式概率提升 */
  protected Map<Integer,Integer> specialMode;
  /** 特殊模式X-Y倍中奖概率提升 */
  protected Map<Integer,Map<Integer,Integer>> specialModeProbUp;
  /** 中奖概率提升（常规模式X-Y倍中奖概率提升） */
  protected Map<Integer,Map<Integer,Integer>> winRate;

  /** 返回成员组成 */
  public List<Integer> getMembers() {
    return Members;
  }

  /** 返回多语言 */
  public int getNameId() {
    return NameId;
  }

  /** 返回属性提升 */
  public int getStatBoost() {
    return StatBoost;
  }

  /** 返回游戏ID */
  public int getGameType() {
    return gameType;
  }

  /** 返回多语言ID */
  public int getLanguageID() {
    return languageID;
  }

  /** 返回进入特殊模式概率提升 */
  public Map<Integer,Integer> getSpecialMode() {
    return specialMode;
  }

  /** 返回特殊模式X-Y倍中奖概率提升 */
  public Map<Integer,Map<Integer,Integer>> getSpecialModeProbUp() {
    return specialModeProbUp;
  }

  /** 返回中奖概率提升（常规模式X-Y倍中奖概率提升） */
  public Map<Integer,Map<Integer,Integer>> getWinRate() {
    return winRate;
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
