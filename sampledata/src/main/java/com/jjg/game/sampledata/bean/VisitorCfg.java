package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName Visitor.xlsx
 * @sheetName Visitor
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class VisitorCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "Visitor.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "Visitor";

  /** 基础停留时长 */
  protected int BaseDwellTime;
  /** 基础刷新权重 */
  protected int BaseWeight;
  /** 建筑交互权重 */
  protected List<List<Integer>> InteractionWeight;
  /** 游客品质 */
  protected int Quality;
  /** 曝光时的刷新权重 */
  protected int RefreshWeights;
  /** 指定区域 */
  protected List<Integer> TargetArea;
  /** 游客 */
  protected int Tourist;
  /** 知名度要求 */
  protected int awareness;
  /** 解锁道具 */
  protected Map<Integer,Integer> unlockitems;

  /** 返回基础停留时长 */
  public int getBaseDwellTime() {
    return BaseDwellTime;
  }

  /** 返回基础刷新权重 */
  public int getBaseWeight() {
    return BaseWeight;
  }

  /** 返回建筑交互权重 */
  public List<List<Integer>> getInteractionWeight() {
    return InteractionWeight;
  }

  /** 返回游客品质 */
  public int getQuality() {
    return Quality;
  }

  /** 返回曝光时的刷新权重 */
  public int getRefreshWeights() {
    return RefreshWeights;
  }

  /** 返回指定区域 */
  public List<Integer> getTargetArea() {
    return TargetArea;
  }

  /** 返回游客 */
  public int getTourist() {
    return Tourist;
  }

  /** 返回知名度要求 */
  public int getAwareness() {
    return awareness;
  }

  /** 返回解锁道具 */
  public Map<Integer,Integer> getUnlockitems() {
    return unlockitems;
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
