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

  /** 游戏id */
  protected int gameType;
  /** 等级_激活后解锁属性id */
  protected Map<Integer,List<Integer>> skillId;
  /** 组内序号 */
  protected int subIdx;
  /** 组别 */
  protected int team;

  /** 返回游戏id */
  public int getGameType() {
    return gameType;
  }

  /** 返回等级_激活后解锁属性id */
  public Map<Integer,List<Integer>> getSkillId() {
    return skillId;
  }

  /** 返回组内序号 */
  public int getSubIdx() {
    return subIdx;
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
