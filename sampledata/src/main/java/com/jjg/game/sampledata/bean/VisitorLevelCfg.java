package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName VisitorLevel.xlsx
 * @sheetName VisitorLevel
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class VisitorLevelCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "VisitorLevel.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "VisitorLevel";

  /** 额外材料掉落 */
  protected List<List<Integer>> BonusRate;
  /** 游客等级 */
  protected int Level;
  /** 固定产出 */
  protected Map<Integer,Integer> Reward;
  /** 游客ID */
  protected int Visitor;
  /** 升级所需经验值 */
  protected int levelUpExp;

  /** 返回额外材料掉落 */
  public List<List<Integer>> getBonusRate() {
    return BonusRate;
  }

  /** 返回游客等级 */
  public int getLevel() {
    return Level;
  }

  /** 返回固定产出 */
  public Map<Integer,Integer> getReward() {
    return Reward;
  }

  /** 返回游客ID */
  public int getVisitor() {
    return Visitor;
  }

  /** 返回升级所需经验值 */
  public int getLevelUpExp() {
    return levelUpExp;
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
