package com.jjg.game.slots.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName Pool.xlsx
 * @sheetName Pool
 * @author Auto.Generator
 * @date 2025年07月05日 14:02:23
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PoolCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "Pool.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "Pool";

  /** 押注抽水入奖池万分比 */
  protected int commissionProp;
  /** 假累计奖池部分进入比率 */
  protected List<Integer> fakeCommissionProp;
  /** 假奖池初始值倍率 */
  protected int fakePoolInitTimes;
  /** 循环的假奖池上限倍率 */
  protected int fakePoolMax;
  /** 循环的假奖池增长速率 */
  protected Map<Integer,String> growthRate;
  /** 奖池系数 */
  protected List<Integer> poolProp;

  /** 返回押注抽水入奖池万分比 */
  public int getCommissionProp() {
    return commissionProp;
  }

  /** 返回假累计奖池部分进入比率 */
  public List<Integer> getFakeCommissionProp() {
    return fakeCommissionProp;
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
  public Map<Integer,String> getGrowthRate() {
    return growthRate;
  }

  /** 返回奖池系数 */
  public List<Integer> getPoolProp() {
    return poolProp;
  }
}
