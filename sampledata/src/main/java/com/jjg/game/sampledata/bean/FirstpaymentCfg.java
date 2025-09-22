package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName FirstPayment.xlsx
 * @sheetName firstpayment
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class FirstpaymentCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "FirstPayment.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "firstpayment";

  /** 头像框奖励 */
  protected Map<Integer,Long> getAvatarFrame;
  /** 金币奖励 */
  protected Map<Integer,Long> getgold;
  /** 道具奖励 */
  protected Map<Integer,Long> getitem;
  /** 购买金额 */
  protected int money;

  /** 返回头像框奖励 */
  public Map<Integer,Long> getGetAvatarFrame() {
    return getAvatarFrame;
  }

  /** 返回金币奖励 */
  public Map<Integer,Long> getGetgold() {
    return getgold;
  }

  /** 返回道具奖励 */
  public Map<Integer,Long> getGetitem() {
    return getitem;
  }

  /** 返回购买金额 */
  public int getMoney() {
    return money;
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
