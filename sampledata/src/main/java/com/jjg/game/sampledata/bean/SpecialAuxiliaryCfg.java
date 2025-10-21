package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName SpecialAuxiliary.xlsx
 * @sheetName SpecialAuxiliary
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SpecialAuxiliaryCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "SpecialAuxiliary.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "SpecialAuxiliary";

  /** 奖励类型A */
  protected List<Integer> awardTypeA;
  /** 奖励类型B */
  protected String awardTypeB;
  /** 单线押分倍数，中奖权重，最大出现次数，根据配置额外增加 */
  protected Map<Integer,Integer> awardTypeC;
  /** 赋予值类型 */
  protected List<Integer> awardTypeC_value;
  /** 游戏ID */
  protected int gameType;
  /** 额外奖励：次数&权重-次数&权重 */
  protected Map<Integer,Integer> randCount;
  /** 此次玩法使用得滚轴组ID */
  protected int rollerMode;
  /** 修改图案策略，改完后再进行中奖判断 */
  protected List<Integer> specialGirdID;
  /** 修改图案策略组，多个修改中按概率抽取1个修改 */
  protected Map<Integer,Integer> specialGroupGirdID;
  /** 旋转次数&权重 */
  protected Map<Integer,Integer> triggerCount;
  /** 客户端玩法模式 */
  protected int type;

  /** 返回奖励类型A */
  public List<Integer> getAwardTypeA() {
    return awardTypeA;
  }

  /** 返回奖励类型B */
  public String getAwardTypeB() {
    return awardTypeB;
  }

  /** 返回单线押分倍数，中奖权重，最大出现次数，根据配置额外增加 */
  public Map<Integer,Integer> getAwardTypeC() {
    return awardTypeC;
  }

  /** 返回赋予值类型 */
  public List<Integer> getAwardTypeC_value() {
    return awardTypeC_value;
  }

  /** 返回游戏ID */
  public int getGameType() {
    return gameType;
  }

  /** 返回额外奖励：次数&权重-次数&权重 */
  public Map<Integer,Integer> getRandCount() {
    return randCount;
  }

  /** 返回此次玩法使用得滚轴组ID */
  public int getRollerMode() {
    return rollerMode;
  }

  /** 返回修改图案策略，改完后再进行中奖判断 */
  public List<Integer> getSpecialGirdID() {
    return specialGirdID;
  }

  /** 返回修改图案策略组，多个修改中按概率抽取1个修改 */
  public Map<Integer,Integer> getSpecialGroupGirdID() {
    return specialGroupGirdID;
  }

  /** 返回旋转次数&权重 */
  public Map<Integer,Integer> getTriggerCount() {
    return triggerCount;
  }

  /** 返回客户端玩法模式 */
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
