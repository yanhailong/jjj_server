package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName ResearchInstitute.xlsx
 * @sheetName ResearchInstitute
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ResearchInstituteCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "ResearchInstitute.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "ResearchInstitute";

  /** 解锁游戏id */
  protected int gameType;
  /** 研究院等级 */
  protected int level;
  /** 属性技能位置坐标 */
  protected Map<Integer,Integer> location;
  /** 组别 */
  protected int team;

  /** 返回解锁游戏id */
  public int getGameType() {
    return gameType;
  }

  /** 返回研究院等级 */
  public int getLevel() {
    return level;
  }

  /** 返回属性技能位置坐标 */
  public Map<Integer,Integer> getLocation() {
    return location;
  }

  /** 返回组别 */
  public int getTeam() {
    return team;
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
