package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName viplevel.xlsx
 * @sheetName viplevel
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ViplevelCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "viplevel.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "viplevel";

  /** 季度奖励 */
  protected Map<Integer,Long> annualRewards;
  /** 等级道具奖励 */
  protected Map<Integer,Long> avatarType;
  /** 生日奖励 */
  protected Map<Integer,Long> birthdayReward;
  /** 等级说明多语言ID */
  protected int describeID;
  /** 有效下注经验转化万分比 */
  protected int effectiveBetting;
  /** 等级金币奖励 */
  protected Map<Integer,Long> levelRewards;
  /** 特权功能 */
  protected Map<Integer,Integer> privilegedFunctions;
  /** 充值金额 */
  protected long rechage;
  /** 充值金额转为经验万分比 */
  protected int recharge;
  /** 回退间隔_回退经验 */
  protected List<Integer> rollback;
  /** 服务器解锁装扮 */
  protected List<Integer> serverAvatarType;
  /** VIP等级 */
  protected int viplevel;
  /** 升级所需经验 */
  protected long viplevelUpExp;
  /** 每周奖励 */
  protected Map<Integer,Long> weeklyRewards;

  /** 返回季度奖励 */
  public Map<Integer,Long> getAnnualRewards() {
    return annualRewards;
  }

  /** 返回等级道具奖励 */
  public Map<Integer,Long> getAvatarType() {
    return avatarType;
  }

  /** 返回生日奖励 */
  public Map<Integer,Long> getBirthdayReward() {
    return birthdayReward;
  }

  /** 返回等级说明多语言ID */
  public int getDescribeID() {
    return describeID;
  }

  /** 返回有效下注经验转化万分比 */
  public int getEffectiveBetting() {
    return effectiveBetting;
  }

  /** 返回等级金币奖励 */
  public Map<Integer,Long> getLevelRewards() {
    return levelRewards;
  }

  /** 返回特权功能 */
  public Map<Integer,Integer> getPrivilegedFunctions() {
    return privilegedFunctions;
  }

  /** 返回充值金额 */
  public long getRechage() {
    return rechage;
  }

  /** 返回充值金额转为经验万分比 */
  public int getRecharge() {
    return recharge;
  }

  /** 返回回退间隔_回退经验 */
  public List<Integer> getRollback() {
    return rollback;
  }

  /** 返回服务器解锁装扮 */
  public List<Integer> getServerAvatarType() {
    return serverAvatarType;
  }

  /** 返回VIP等级 */
  public int getViplevel() {
    return viplevel;
  }

  /** 返回升级所需经验 */
  public long getViplevelUpExp() {
    return viplevelUpExp;
  }

  /** 返回每周奖励 */
  public Map<Integer,Long> getWeeklyRewards() {
    return weeklyRewards;
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
