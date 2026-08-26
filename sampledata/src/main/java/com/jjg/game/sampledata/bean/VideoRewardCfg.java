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

  /** 礼包内容 */
  protected Map<Integer,Long> Rewards;
  /** 观看次数 */
  protected int VideoCount;

  /** 返回礼包内容 */
  public Map<Integer,Long> getRewards() {
    return Rewards;
  }

  /** 返回观看次数 */
  public int getVideoCount() {
    return VideoCount;
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
