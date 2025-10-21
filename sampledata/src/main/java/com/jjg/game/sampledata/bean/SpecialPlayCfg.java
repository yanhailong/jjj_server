package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName SpecialPlay.xlsx
 * @sheetName SpecialPlay
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SpecialPlayCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "SpecialPlay.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "SpecialPlay";

  /** 游戏ID */
  protected int gameType;
  /** 玩法关键字 */
  protected int playType;
  /** 标记全部触发后给与奖励 */
  protected List<Integer> triggerRewards;
  /** 触发标记_可触发次数 */
  protected Map<Integer,Integer> triggerTag;
  /** 值 */
  protected String value;

  /** 返回游戏ID */
  public int getGameType() {
    return gameType;
  }

  /** 返回玩法关键字 */
  public int getPlayType() {
    return playType;
  }

  /** 返回标记全部触发后给与奖励 */
  public List<Integer> getTriggerRewards() {
    return triggerRewards;
  }

  /** 返回触发标记_可触发次数 */
  public Map<Integer,Integer> getTriggerTag() {
    return triggerTag;
  }

  /** 返回值 */
  public String getValue() {
    return value;
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
