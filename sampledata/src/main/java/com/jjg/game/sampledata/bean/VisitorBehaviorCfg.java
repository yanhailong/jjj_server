package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName VisitorBehavior.xlsx
 * @sheetName VisitorBehavior
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class VisitorBehaviorCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "VisitorBehavior.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "VisitorBehavior";

  /** 基础停留时长 */
  protected int BaseDwellTime;
  /** 建筑交互权重 */
  protected List<List<Integer>> InteractionWeight;
  /** 游客等级 */
  protected int Level;
  /** 交互产出 */
  protected List<List<Integer>> Reward;
  /** 指定区域 */
  protected List<Integer> TargetArea;
  /** 升级所需经验值 */
  protected int levelUpExp;

  /** 返回基础停留时长 */
  public int getBaseDwellTime() {
    return BaseDwellTime;
  }

  /** 返回建筑交互权重 */
  public List<List<Integer>> getInteractionWeight() {
    return InteractionWeight;
  }

  /** 返回游客等级 */
  public int getLevel() {
    return Level;
  }

  /** 返回交互产出 */
  public List<List<Integer>> getReward() {
    return Reward;
  }

  /** 返回指定区域 */
  public List<Integer> getTargetArea() {
    return TargetArea;
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
