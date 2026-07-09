package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName ResearchSkills.xlsx
 * @sheetName ResearchSkills
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ResearchSkillsCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "ResearchSkills.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "ResearchSkills";

  /** 属性id */
  protected int Attr;
  /** 自动旋转次数 */
  protected int AutoSpin;
  /** 战力值 */
  protected int CombatPower;
  /** 每级所需研究点 */
  protected Map<Integer,Integer> ResearchPoints;
  /** 快速旋转 */
  protected int TurboSpin;
  /** 金额解锁 */
  protected List<Long> bet;
  /** 游戏id */
  protected int gameType;
  /** 研究等级 */
  protected int grade;
  /** 多语言id */
  protected int languageID;
  /** 进入特殊模式概率提升 */
  protected Map<Integer,Integer> specialMode;
  /** 特殊模式X-Y倍中奖概率提升 */
  protected Map<Integer,Map<Integer,Integer>> specialModeProbUp;
  /** 中奖概率提升（常规模式X-Y倍中奖概率提升） */
  protected Map<Integer,Map<Integer,Integer>> winRate;

  /** 返回属性id */
  public int getAttr() {
    return Attr;
  }

  /** 返回自动旋转次数 */
  public int getAutoSpin() {
    return AutoSpin;
  }

  /** 返回战力值 */
  public int getCombatPower() {
    return CombatPower;
  }

  /** 返回每级所需研究点 */
  public Map<Integer,Integer> getResearchPoints() {
    return ResearchPoints;
  }

  /** 返回快速旋转 */
  public int getTurboSpin() {
    return TurboSpin;
  }

  /** 返回金额解锁 */
  public List<Long> getBet() {
    return bet;
  }

  /** 返回游戏id */
  public int getGameType() {
    return gameType;
  }

  /** 返回研究等级 */
  public int getGrade() {
    return grade;
  }

  /** 返回多语言id */
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
