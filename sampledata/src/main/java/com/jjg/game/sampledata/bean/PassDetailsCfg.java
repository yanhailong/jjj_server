package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName PassDetails.xlsx
 * @sheetName PassDetails
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PassDetailsCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "PassDetails.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "PassDetails";

  /** 初级付费奖励 */
  protected Map<Integer,Long> BasicPaidRewards;
  /** 完成条件 */
  protected List<Long> CompletionCondition;
  /** 免费奖励 */
  protected Map<Integer,Long> FreeRewards;
  /** 通行证 */
  protected int PassID;
  /** 高级付费奖励 */
  protected Map<Integer,Long> PremiumPaidRewards;
  /** 内容描述 */
  protected int language;
  /** 等级 */
  protected int level;

  /** 返回初级付费奖励 */
  public Map<Integer,Long> getBasicPaidRewards() {
    return BasicPaidRewards;
  }

  /** 返回完成条件 */
  public List<Long> getCompletionCondition() {
    return CompletionCondition;
  }

  /** 返回免费奖励 */
  public Map<Integer,Long> getFreeRewards() {
    return FreeRewards;
  }

  /** 返回通行证 */
  public int getPassID() {
    return PassID;
  }

  /** 返回高级付费奖励 */
  public Map<Integer,Long> getPremiumPaidRewards() {
    return PremiumPaidRewards;
  }

  /** 返回内容描述 */
  public int getLanguage() {
    return language;
  }

  /** 返回等级 */
  public int getLevel() {
    return level;
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
