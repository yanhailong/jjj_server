package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName prop.xlsx
 * @sheetName prop
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PropCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "prop.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "prop";

  /** 位置排序 */
  protected int SortOrder;
  /** 游戏id */
  protected int gameType;
  /** 属性图标 */
  protected String icon;
  /** 多语言id */
  protected int languageID;
  /** 解锁属性id_等级 */
  protected Map<Integer,Integer> skillId;
  /** 类型 */
  protected int type;

  /** 返回位置排序 */
  public int getSortOrder() {
    return SortOrder;
  }

  /** 返回游戏id */
  public int getGameType() {
    return gameType;
  }

  /** 返回属性图标 */
  public String getIcon() {
    return icon;
  }

  /** 返回多语言id */
  public int getLanguageID() {
    return languageID;
  }

  /** 返回解锁属性id_等级 */
  public Map<Integer,Integer> getSkillId() {
    return skillId;
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
