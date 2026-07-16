package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName MiningCellType.xlsx
 * @sheetName MiningCellType
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class MiningCellTypeCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "MiningCellType.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "MiningCellType";

  /** 出现的深度 */
  protected List<Integer> DropDepth;
  /** 格子名称 */
  protected int GridName;
  /** 耐久 */
  protected int HP;
  /** 奖励 */
  protected List<Integer> Reward;
  /** 客户端资源 */
  protected String icon;

  /** 返回出现的深度 */
  public List<Integer> getDropDepth() {
    return DropDepth;
  }

  /** 返回格子名称 */
  public int getGridName() {
    return GridName;
  }

  /** 返回耐久 */
  public int getHP() {
    return HP;
  }

  /** 返回奖励 */
  public List<Integer> getReward() {
    return Reward;
  }

  /** 返回客户端资源 */
  public String getIcon() {
    return icon;
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
