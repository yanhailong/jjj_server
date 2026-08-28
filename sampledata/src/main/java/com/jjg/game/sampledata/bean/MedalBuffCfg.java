package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName MedalBuff.xlsx
 * @sheetName MedalBuff
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class MedalBuffCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "MedalBuff.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "MedalBuff";

  /** 加成效果 */
  protected Map<Integer,Integer> BuffId;
  /** BUFF描述 */
  protected List<Integer> BuffStr;
  /** 收集数量目标 */
  protected int CollectNum;
  /** 徽章ID */
  protected int MedalType;
  /** 建筑ID */
  protected int buildID;

  /** 返回加成效果 */
  public Map<Integer,Integer> getBuffId() {
    return BuffId;
  }

  /** 返回BUFF描述 */
  public List<Integer> getBuffStr() {
    return BuffStr;
  }

  /** 返回收集数量目标 */
  public int getCollectNum() {
    return CollectNum;
  }

  /** 返回徽章ID */
  public int getMedalType() {
    return MedalType;
  }

  /** 返回建筑ID */
  public int getBuildID() {
    return buildID;
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
