package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName VisitorQuest.xlsx
 * @sheetName VisitorQuest
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class VisitorQuestCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "VisitorQuest.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "VisitorQuest";

  /** 无奖励的服务能力 */
  protected int BaseServiceCapacity;
  /** 基础刷新权重 */
  protected int BaseWeight;
  /** 道具ID_碎片ID_碎片数量 */
  protected List<Integer> DuplicatetoShard;
  /** 建筑交互权重 */
  protected Map<Integer,Integer> InteractionWeight;
  /** 游客品质 */
  protected int Quality;
  /** 资源 */
  protected int Resource;
  /** 服务能力 */
  protected int ServiceCapacity;
  /** 指定区域 */
  protected List<Integer> TargetArea;
  /** 知名度 */
  protected int awareness;

  /** 返回无奖励的服务能力 */
  public int getBaseServiceCapacity() {
    return BaseServiceCapacity;
  }

  /** 返回基础刷新权重 */
  public int getBaseWeight() {
    return BaseWeight;
  }

  /** 返回道具ID_碎片ID_碎片数量 */
  public List<Integer> getDuplicatetoShard() {
    return DuplicatetoShard;
  }

  /** 返回建筑交互权重 */
  public Map<Integer,Integer> getInteractionWeight() {
    return InteractionWeight;
  }

  /** 返回游客品质 */
  public int getQuality() {
    return Quality;
  }

  /** 返回资源 */
  public int getResource() {
    return Resource;
  }

  /** 返回服务能力 */
  public int getServiceCapacity() {
    return ServiceCapacity;
  }

  /** 返回指定区域 */
  public List<Integer> getTargetArea() {
    return TargetArea;
  }

  /** 返回知名度 */
  public int getAwareness() {
    return awareness;
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
