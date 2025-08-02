package com.jjg.game.slots.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName SpecialGird.xlsx
 * @sheetName SpecialGird
 * @author Auto.Generator
 * @date 2025年08月02日 14:18:48
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SpecialGirdCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "SpecialGird.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "SpecialGird";

  /** 影响格子 */
  protected List<List<Integer>> affectGird;
  /** 是否在别的格子修改后才生效 */
  protected boolean afterUpdateValid;
  /** 巨型块大小 */
  protected List<List<String>> bigBlockSize;
  /** 元素 */
  protected Map<Integer,Integer> element;
  /** 是否额外投注 */
  protected boolean extraBet;
  /** 游戏ID */
  protected int gameType;
  /** 格式修改算法 */
  protected int girdUpdateType;
  /** 模式ID */
  protected int modelId;
  /** 不替换元素列表 */
  protected List<Integer> notReplaceEle;
  /** 随机次数 */
  protected Map<Integer,Integer> randCount;
  /** 旋转状态 */
  protected int spinStatus;
  /** 旋转标识 */
  protected int spinType;
  /** 是否同步格子 */
  protected boolean syncGird;

  /** 返回影响格子 */
  public List<List<Integer>> getAffectGird() {
    return affectGird;
  }

  /** 返回是否在别的格子修改后才生效 */
  public boolean getAfterUpdateValid() {
    return afterUpdateValid;
  }

  /** 返回巨型块大小 */
  public List<List<String>> getBigBlockSize() {
    return bigBlockSize;
  }

  /** 返回元素 */
  public Map<Integer,Integer> getElement() {
    return element;
  }

  /** 返回是否额外投注 */
  public boolean getExtraBet() {
    return extraBet;
  }

  /** 返回游戏ID */
  public int getGameType() {
    return gameType;
  }

  /** 返回格式修改算法 */
  public int getGirdUpdateType() {
    return girdUpdateType;
  }

  /** 返回模式ID */
  public int getModelId() {
    return modelId;
  }

  /** 返回不替换元素列表 */
  public List<Integer> getNotReplaceEle() {
    return notReplaceEle;
  }

  /** 返回随机次数 */
  public Map<Integer,Integer> getRandCount() {
    return randCount;
  }

  /** 返回旋转状态 */
  public int getSpinStatus() {
    return spinStatus;
  }

  /** 返回旋转标识 */
  public int getSpinType() {
    return spinType;
  }

  /** 返回是否同步格子 */
  public boolean getSyncGird() {
    return syncGird;
  }
}
