package com.jjg.game.slots.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName SpecialAuxiliary.xlsx
 * @sheetName SpecialAuxiliary
 * @author Auto.Generator
 * @date 2025年07月28日 11:01:45
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SpecialAuxiliaryCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "SpecialAuxiliary.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "SpecialAuxiliary";

  /** 客户端显示数量 */
  protected Map<Integer,Integer> clientShowCount;
  /** 免费的固定奖励 */
  protected List<List<Integer>> freeAward;
  /** 免费随机奖励 */
  protected Map<Integer,List<List<Integer>>> freeRandAward;
  /** 游戏ID */
  protected int gameType;
  /** 是否有交互 */
  protected boolean interflow;
  /** 随机次数 */
  protected Map<Integer,Map<Integer,Integer>> randCount;
  /** 旋转状态标识 */
  protected int spinType;
  /** 模式 */
  protected int type;

  /** 返回客户端显示数量 */
  public Map<Integer,Integer> getClientShowCount() {
    return clientShowCount;
  }

  /** 返回免费的固定奖励 */
  public List<List<Integer>> getFreeAward() {
    return freeAward;
  }

  /** 返回免费随机奖励 */
  public Map<Integer,List<List<Integer>>> getFreeRandAward() {
    return freeRandAward;
  }

  /** 返回游戏ID */
  public int getGameType() {
    return gameType;
  }

  /** 返回是否有交互 */
  public boolean getInterflow() {
    return interflow;
  }

  /** 返回随机次数 */
  public Map<Integer,Map<Integer,Integer>> getRandCount() {
    return randCount;
  }

  /** 返回旋转状态标识 */
  public int getSpinType() {
    return spinType;
  }

  /** 返回模式 */
  public int getType() {
    return type;
  }
}
