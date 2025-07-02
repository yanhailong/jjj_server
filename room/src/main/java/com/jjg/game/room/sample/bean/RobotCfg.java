package com.jjg.game.room.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName robot.xlsx
 * @sheetName Robot
 * @author Auto.Generator
 * @date 2025年07月21日 14:06:29
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class RobotCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "robot.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "Robot";

  /** 补充金币 */
  protected List<List<Integer>> AddMoney;
  /** 是否可用 */
  protected int Available;
  /** 押注类行为策略ID */
  protected List<List<Integer>> BetRobotID;
  /** 对战类行为策略ID */
  protected List<List<Integer>> ChessRobotID;
  /** 每次局游戏结束时退出概率 */
  protected int Exit;
  /** 退出房间时倍数 */
  protected int ExitMultiplier;
  /** 携带基础金币 */
  protected int Money;
  /** 机器人名字 */
  protected String Name;
  /** 头像资源名 */
  protected String NameIcon;
  /** VIP等级 */
  protected int VipLevel;

  /** 返回补充金币 */
  public List<List<Integer>> getAddMoney() {
    return AddMoney;
  }

  /** 返回是否可用 */
  public int getAvailable() {
    return Available;
  }

  /** 返回押注类行为策略ID */
  public List<List<Integer>> getBetRobotID() {
    return BetRobotID;
  }

  /** 返回对战类行为策略ID */
  public List<List<Integer>> getChessRobotID() {
    return ChessRobotID;
  }

  /** 返回每次局游戏结束时退出概率 */
  public int getExit() {
    return Exit;
  }

  /** 返回退出房间时倍数 */
  public int getExitMultiplier() {
    return ExitMultiplier;
  }

  /** 返回携带基础金币 */
  public int getMoney() {
    return Money;
  }

  /** 返回机器人名字 */
  public String getName() {
    return Name;
  }

  /** 返回头像资源名 */
  public String getNameIcon() {
    return NameIcon;
  }

  /** 返回VIP等级 */
  public int getVipLevel() {
    return VipLevel;
  }
}
