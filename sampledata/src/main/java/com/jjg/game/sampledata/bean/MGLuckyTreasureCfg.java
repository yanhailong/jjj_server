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
  protected int BestValue;
  /** 单份需要道具数(ID_NUM) */
  protected List<Integer> Consumption;
  /** 图片资源 */
  protected String Des;
  /** 商品ID */
  protected int ItemId;
  /** 商品数量 */
  protected int ItemNum;
  /** 总份数 */
  protected int Total;
  /** 领奖时间(分) */
  protected int collectime;
  /** 商品名称 */
  protected String name;
  /** 每局抢购时间(分钟) */
  protected int time;
  /** 类型 */
  protected int type;

  /** 返回商品价值 */
  public int getBestValue() {
    return BestValue;
  }

  /** 返回单份需要道具数(ID_NUM) */
  public List<Integer> getConsumption() {
    return Consumption;
  }

  /** 返回图片资源 */
  public String getDes() {
    return Des;
  }

  /** 返回商品ID */
  public int getItemId() {
    return ItemId;
  }

  /** 返回商品数量 */
  public int getItemNum() {
    return ItemNum;
  }

  /** 返回总份数 */
  public int getTotal() {
    return Total;
  }

  /** 返回领奖时间(分) */
  public int getCollectime() {
    return collectime;
  }

  /** 返回商品名称 */
  public String getName() {
    return name;
  }

  /** 返回每局抢购时间(分钟) */
  public int getTime() {
    return time;
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
