package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName AirstrikeRobot‌.xlsx
 * @sheetName AirstrikeRobot‌
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class AirstrikeRobotCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "AirstrikeRobot‌.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "AirstrikeRobot‌";

  /** 兑现人数万分比/s */
  protected List<Integer> CashIn;
  /** 增加人数万分比/s */
  protected List<Integer> Increase;
  /** 初始人数 */
  protected List<Integer> Initial;
  /** 第二轮下注人数万分比 */
  protected List<Integer> PeopleNum;
  /** 机器人下注金额 */
  protected List<List<Integer>> stake;

  /** 返回兑现人数万分比/s */
  public List<Integer> getCashIn() {
    return CashIn;
  }

  /** 返回增加人数万分比/s */
  public List<Integer> getIncrease() {
    return Increase;
  }

  /** 返回初始人数 */
  public List<Integer> getInitial() {
    return Initial;
  }

  /** 返回第二轮下注人数万分比 */
  public List<Integer> getPeopleNum() {
    return PeopleNum;
  }

  /** 返回机器人下注金额 */
  public List<List<Integer>> getStake() {
    return stake;
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
