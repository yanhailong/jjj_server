package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName PointsAwardRanking.xlsx
 * @sheetName PointsAwardRanking
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PointsAwardRankingCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "PointsAwardRanking.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "PointsAwardRanking";

  /** 奖励类型 */
  protected int awardType;
  /** 排名奖励 */
  protected List<String> getItem;
  /** 奖励价值 */
  protected long price;
  /** 排名名次 */
  protected List<Integer> ranking;
  /** 开始日期 */
  protected String time;
  /** 排行类型 */
  protected int type;

  /** 返回奖励类型 */
  public int getAwardType() {
    return awardType;
  }

  /** 返回排名奖励 */
  public List<String> getGetItem() {
    return getItem;
  }

  /** 返回奖励价值 */
  public long getPrice() {
    return price;
  }

  /** 返回排名名次 */
  public List<Integer> getRanking() {
    return ranking;
  }

  /** 返回开始日期 */
  public String getTime() {
    return time;
  }

  /** 返回排行类型 */
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
