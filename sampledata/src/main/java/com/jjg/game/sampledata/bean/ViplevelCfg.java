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

  /** 年奖励 */
  protected Map<Integer,Long> annualRewards;
  /** 新解锁装扮 */
  protected Map<Integer,Integer> avatarType;
  /** 生日奖励 */
  protected Map<Integer,Long> birthdayReward;
  /** 等级说明多语言ID */
  protected int describeID;
  /** 有效下注经验转化万分比 */
  protected int effectiveBetting;
  /** 等级奖励 */
  protected Map<Integer,Long> levelRewards;
  /** 特权功能 */
  protected Map<Integer,Long> privilegedFunctions;
  /** 充值金额转为经验万分比 */
  protected int recharge;
  /** 回退间隔_回退经验 */
  protected List<Integer> rollback;
  /** VIP等级 */
  protected int viplevel;
  /** 升级所需经验 */
  protected long viplevelUpExp;
  /** 每周奖励 */
  protected Map<Integer,Long> weeklyRewards;

  /** 返回年奖励 */
  public Map<Integer,Long> getAnnualRewards() {
    return annualRewards;
  }

  /** 返回新解锁装扮 */
  public Map<Integer,Integer> getAvatarType() {
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

  /** 返回等级奖励 */
  public Map<Integer,Long> getLevelRewards() {
    return levelRewards;
  }

  /** 返回特权功能 */
  public Map<Integer,Long> getPrivilegedFunctions() {
    return privilegedFunctions;
  }

  /** 返回充值金额转为经验万分比 */
  public int getRecharge() {
    return recharge;
  }

  /** 返回回退间隔_回退经验 */
  public List<Integer> getRollback() {
    return rollback;
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
