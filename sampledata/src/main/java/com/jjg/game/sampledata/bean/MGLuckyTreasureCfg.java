package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName MGLuckyTreasure.xlsx
 * @sheetName MGLuckyTreasure
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class MGLuckyTreasureCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "MGLuckyTreasure.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "MGLuckyTreasure";

  /** 商品价值 */
  protected int bestValue;
  /** 领奖时间(分) */
  protected int collecTime;
  /** 单份道具数 */
  protected List<Integer> consumption;
  /** 商品图片资源 */
  protected String des;
  /** 开启状态 */
  protected boolean isOpen;
  /** 商品ID */
  protected int itemId;
  /** 商品数量 */
  protected int itemNum;
  /** 商品名称 */
  protected String name;
  /** 机器人购买上限万分比 */
  protected int robotHaveMax;
  /** 单次购买分数万分比下限_上限 */
  protected List<Integer> robotSinglePurchase;
  /** 购买间隔时间下限_上限（毫秒） */
  protected List<Integer> robotTime;
  /** 每局抢购时间(分钟) */
  protected int time;
  /** 总份数 */
  protected int total;
  /** 类型 */
  protected int type;

  /** 返回商品价值 */
  public int getBestValue() {
    return bestValue;
  }

  /** 返回领奖时间(分) */
  public int getCollecTime() {
    return collecTime;
  }

  /** 返回单份道具数 */
  public List<Integer> getConsumption() {
    return consumption;
  }

  /** 返回商品图片资源 */
  public String getDes() {
    return des;
  }

  /** 返回开启状态 */
  public boolean getIsOpen() {
    return isOpen;
  }

  /** 返回商品ID */
  public int getItemId() {
    return itemId;
  }

  /** 返回商品数量 */
  public int getItemNum() {
    return itemNum;
  }

  /** 返回商品名称 */
  public String getName() {
    return name;
  }

  /** 返回机器人购买上限万分比 */
  public int getRobotHaveMax() {
    return robotHaveMax;
  }

  /** 返回单次购买分数万分比下限_上限 */
  public List<Integer> getRobotSinglePurchase() {
    return robotSinglePurchase;
  }

  /** 返回购买间隔时间下限_上限（毫秒） */
  public List<Integer> getRobotTime() {
    return robotTime;
  }

  /** 返回每局抢购时间(分钟) */
  public int getTime() {
    return time;
  }

  /** 返回总份数 */
  public int getTotal() {
    return total;
  }

  /** 返回类型 */
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
