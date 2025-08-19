package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName robot.xlsx
 * @sheetName Robot
 * @author Auto.Generator
 * @date 2025年08月19日 11:16:26
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class RobotCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "robot.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "Robot";

  /** 补充金币 */
  protected List<List<Long>> addMoney;
  /** 是否可用 */
  protected int available;
  /** 押注类行为策略ID */
  protected List<List<Integer>> betRobotID;
  /** 对战类行为策略ID */
  protected List<List<Integer>> chessRobotID;
  /** 每次局游戏结束时退出概率 */
  protected int exit;
  /** 退出房间时倍数 */
  protected int exitMultiplier;
  /** 携带基础金币 */
  protected long money;
  /** 机器人名字 */
  protected String name;
  /** 头像资源名 */
  protected String nameIcon;
  /** VIP等级 */
  protected int vipLevel;

  /** 返回补充金币 */
  public List<List<Long>> getAddMoney() {
    return addMoney;
  }

  /** 返回是否可用 */
  public int getAvailable() {
    return available;
  }

  /** 返回押注类行为策略ID */
  public List<List<Integer>> getBetRobotID() {
    return betRobotID;
  }

  /** 返回对战类行为策略ID */
  public List<List<Integer>> getChessRobotID() {
    return chessRobotID;
  }

  /** 返回每次局游戏结束时退出概率 */
  public int getExit() {
    return exit;
  }

  /** 返回退出房间时倍数 */
  public int getExitMultiplier() {
    return exitMultiplier;
  }

  /** 返回携带基础金币 */
  public long getMoney() {
    return money;
  }

  /** 返回机器人名字 */
  public String getName() {
    return name;
  }

  /** 返回头像资源名 */
  public String getNameIcon() {
    return nameIcon;
  }

  /** 返回VIP等级 */
  public int getVipLevel() {
    return vipLevel;
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
