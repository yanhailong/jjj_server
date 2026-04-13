package com.jjg.game.sampledata.bean;

import javax.annotation.processing.Generated;
import java.util.List;
import java.util.Map;

/**
 * 配置bean
 *
 * @excelName SpecialResultLib.xlsx
 * @sheetName SpecialResultLib
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SpecialResultLibCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "SpecialResultLib.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "SpecialResultLib";

  /** 根据用户累计下注次数权重修改（新用户） */
  protected Map<Integer,List<String>> accumulate;
  /** 进入条件上限 */
  protected int enterLimitMax;
  /** 进入条件下线 */
  protected int enterLimitMin;
  /** 游戏ID */
  protected int gameType;
  /** 根据用户标记权重修改（SVIP特殊用户） */
  protected Map<Integer,List<String>> mark;
  /** 调控序列ID */
  protected int modelId;
  /** 根据用户连续未中奖次数权重修改 */
  protected Map<Integer,List<String>> prizeless;
  /** 倍数区间及权重 */
  protected Map<Integer,List<String>> sectionProp;
  /** 类型权重 */
  protected Map<Integer,Integer> typeProp;
  /** 模式ID对应的倍数权重修改 */
  protected List<List<List<String>>> weightChange;

  /** 返回根据用户累计下注次数权重修改（新用户） */
  public Map<Integer,List<String>> getAccumulate() {
    return accumulate;
  }

  /** 返回进入条件上限 */
  public int getEnterLimitMax() {
    return enterLimitMax;
  }

  /** 返回进入条件下线 */
  public int getEnterLimitMin() {
    return enterLimitMin;
  }

  /** 返回游戏ID */
  public int getGameType() {
    return gameType;
  }

  /** 返回根据用户标记权重修改（SVIP特殊用户） */
  public Map<Integer,List<String>> getMark() {
    return mark;
  }

  /** 返回调控序列ID */
  public int getModelId() {
    return modelId;
  }

  /** 返回根据用户连续未中奖次数权重修改 */
  public Map<Integer,List<String>> getPrizeless() {
    return prizeless;
  }

  /** 返回倍数区间及权重 */
  public Map<Integer,List<String>> getSectionProp() {
    return sectionProp;
  }

  /** 返回类型权重 */
  public Map<Integer,Integer> getTypeProp() {
    return typeProp;
  }

  /** 返回模式ID对应的倍数权重修改 */
  public List<List<List<String>>> getWeightChange() {
    return weightChange;
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
