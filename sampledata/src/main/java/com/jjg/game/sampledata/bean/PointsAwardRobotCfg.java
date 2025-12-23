package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName PointsAwardRobot.xlsx
 * @sheetName PointsAwardRobot
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PointsAwardRobotCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "PointsAwardRobot.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "PointsAwardRobot";

  /** 国旗 */
  protected int flag;
  /** 头像框 */
  protected int frame;
  /** 性别 */
  protected int gender;
  /** 机器人名称 */
  protected String nameId;
  /** 头像 */
  protected int picture;
  /** 玩家等级 */
  protected int playerLevel;
  /** 用途 */
  protected List<Integer> usetype;
  /** VIP等级 */
  protected int vipLevel;

  /** 返回国旗 */
  public int getFlag() {
    return flag;
  }

  /** 返回头像框 */
  public int getFrame() {
    return frame;
  }

  /** 返回性别 */
  public int getGender() {
    return gender;
  }

  /** 返回机器人名称 */
  public String getNameId() {
    return nameId;
  }

  /** 返回头像 */
  public int getPicture() {
    return picture;
  }

  /** 返回玩家等级 */
  public int getPlayerLevel() {
    return playerLevel;
  }

  /** 返回用途 */
  public List<Integer> getUsetype() {
    return usetype;
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
