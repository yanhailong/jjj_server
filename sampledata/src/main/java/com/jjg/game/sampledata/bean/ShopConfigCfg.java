package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName shop.xlsx
 * @sheetName ShopConfig
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ShopConfigCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "shop.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "ShopConfig";

  /** 原价值 */
  protected Map<Integer,Long> discount;
  /** 结束日期 */
  protected Date endTime;
  /** 道具奖励 */
  protected Map<Integer,Long> item;
  /** 标签1 */
  protected String label1;
  /** 标签2 */
  protected int label2;
  /** 解锁条件 */
  protected Map<Integer,Integer> level;
  /** 购买价格 */
  protected int money;
  /** 是否开启 */
  protected boolean open;
  /** 购买类型 */
  protected int paytype;
  /** 图标 */
  protected String picName;
  /** 开始日期 */
  protected Date startTime;
  /** 分类 */
  protected int type;

  /** 返回原价值 */
  public Map<Integer,Long> getDiscount() {
    return discount;
  }

  /** 返回结束日期 */
  public Date getEndTime() {
    return endTime;
  }

  /** 返回道具奖励 */
  public Map<Integer,Long> getItem() {
    return item;
  }

  /** 返回标签1 */
  public String getLabel1() {
    return label1;
  }

  /** 返回标签2 */
  public int getLabel2() {
    return label2;
  }

  /** 返回解锁条件 */
  public Map<Integer,Integer> getLevel() {
    return level;
  }

  /** 返回购买价格 */
  public int getMoney() {
    return money;
  }

  /** 返回是否开启 */
  public boolean getOpen() {
    return open;
  }

  /** 返回购买类型 */
  public int getPaytype() {
    return paytype;
  }

  /** 返回图标 */
  public String getPicName() {
    return picName;
  }

  /** 返回开始日期 */
  public Date getStartTime() {
    return startTime;
  }

  /** 返回分类 */
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
