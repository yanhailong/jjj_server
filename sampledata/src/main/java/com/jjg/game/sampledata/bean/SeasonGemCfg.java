package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName SeasonGem.xlsx
 * @sheetName SeasonGem
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SeasonGemCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "SeasonGem.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "SeasonGem";

  /** 宝石描述 */
  protected int GemDesc;
  /** 宝石名称 */
  protected int GemName;
  /** 流派 */
  protected int Genre;
  /** 品质 */
  protected int Rarity;
  /** 提升效率（百分比） */
  protected int buff;
  /** 客户端资源 */
  protected String icon;
  /** 类型 */
  protected int type;

  /** 返回宝石描述 */
  public int getGemDesc() {
    return GemDesc;
  }

  /** 返回宝石名称 */
  public int getGemName() {
    return GemName;
  }

  /** 返回流派 */
  public int getGenre() {
    return Genre;
  }

  /** 返回品质 */
  public int getRarity() {
    return Rarity;
  }

  /** 返回提升效率（百分比） */
  public int getBuff() {
    return buff;
  }

  /** 返回客户端资源 */
  public String getIcon() {
    return icon;
  }

  /** 返回类型 */
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
