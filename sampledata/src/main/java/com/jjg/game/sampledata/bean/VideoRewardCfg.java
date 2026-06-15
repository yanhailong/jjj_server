package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName VideoReward.xlsx
 * @sheetName VideoReward
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class VideoRewardCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "VideoReward.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "VideoReward";

  /** 档位次数 */
  protected int Count;
  /** 奖励 */
  protected Map<Integer,Integer> Reward;

  /** 返回档位次数 */
  public int getCount() {
    return Count;
  }

  /** 返回奖励 */
  public Map<Integer,Integer> getReward() {
    return Reward;
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
