package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName SpecialGird.xlsx
 * @sheetName SpecialGird
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SpecialGirdCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "SpecialGird.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "SpecialGird";

  /** 格子ID_格子权重_格子次数 */
  protected List<List<Integer>> affectGird;
  /** 需要出现的元素图案：权重 */
  protected Map<Integer,Integer> element;
  /** 游戏ID */
  protected int gameType;
  /** 不可替换元素列表 */
  protected List<Integer> notReplaceEle;
  /** 随机次数 */
  protected Map<Integer,Integer> randCount;
  /** 每次修改成功后赋予值 */
  protected Map<Integer,Integer> value;
  /** 每次修改成功后赋予值类型 */
  protected List<Integer> valueType;

  /** 返回格子ID_格子权重_格子次数 */
  public List<List<Integer>> getAffectGird() {
    return affectGird;
  }

  /** 返回需要出现的元素图案：权重 */
  public Map<Integer,Integer> getElement() {
    return element;
  }

  /** 返回游戏ID */
  public int getGameType() {
    return gameType;
  }

  /** 返回不可替换元素列表 */
  public List<Integer> getNotReplaceEle() {
    return notReplaceEle;
  }

  /** 返回随机次数 */
  public Map<Integer,Integer> getRandCount() {
    return randCount;
  }

  /** 返回每次修改成功后赋予值 */
  public Map<Integer,Integer> getValue() {
    return value;
  }

  /** 返回每次修改成功后赋予值类型 */
  public List<Integer> getValueType() {
    return valueType;
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
