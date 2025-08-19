package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName Pool.xlsx
 * @sheetName Pool
 * @author Auto.Generator
 * @date 2025年08月19日 15:29:43
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PoolCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "Pool.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "Pool";

  /** 中奖表现延迟时间 */
  protected int delayTime;
  /** 假奖池初始值倍率 */
  protected int fakePoolInitTimes;
  /** 循环的假奖池上限倍率 */
  protected int fakePoolMax;
  /** 循环的假奖池增长速率 */
  protected List<Integer> growthRate;
  /** 奖池系数 */
  protected int poolProp;
  /** 真奖池奖金万分比 */
  protected int truePool;

  /** 返回中奖表现延迟时间 */
  public int getDelayTime() {
    return delayTime;
  }

  /** 返回假奖池初始值倍率 */
  public int getFakePoolInitTimes() {
    return fakePoolInitTimes;
  }

  /** 返回循环的假奖池上限倍率 */
  public int getFakePoolMax() {
    return fakePoolMax;
  }

  /** 返回循环的假奖池增长速率 */
  public List<Integer> getGrowthRate() {
    return growthRate;
  }

  /** 返回奖池系数 */
  public int getPoolProp() {
    return poolProp;
  }

  /** 返回真奖池奖金万分比 */
  public int getTruePool() {
    return truePool;
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
