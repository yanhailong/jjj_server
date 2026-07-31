package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName VisitorPool.xlsx
 * @sheetName VisitorPool
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class VisitorPoolCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "VisitorPool.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "VisitorPool";

  /** 新手引导抽奖 */
  protected List<Integer> NewbieGuideDraw;
  /** 权重_道具ID_数量 */
  protected List<List<Integer>> detailedDropItem;

  /** 返回新手引导抽奖 */
  public List<Integer> getNewbieGuideDraw() {
    return NewbieGuideDraw;
  }

  /** 返回权重_道具ID_数量 */
  public List<List<Integer>> getDetailedDropItem() {
    return detailedDropItem;
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
