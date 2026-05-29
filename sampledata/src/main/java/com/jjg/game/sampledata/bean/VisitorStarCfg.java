package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName VisitorStar.xlsx
 * @sheetName VisitorStar
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class VisitorStarCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "VisitorStar.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "VisitorStar";

  /** 服务能力加成系数 */
  protected int Additioncoefficient;
  /** 升星所需碎片 */
  protected int Ascend;
  /** 概率 */
  protected int Probability;
  /** 游客星级 */
  protected int Starlevel;
  /** 游客ID */
  protected int Visitor;

  /** 返回服务能力加成系数 */
  public int getAdditioncoefficient() {
    return Additioncoefficient;
  }

  /** 返回升星所需碎片 */
  public int getAscend() {
    return Ascend;
  }

  /** 返回概率 */
  public int getProbability() {
    return Probability;
  }

  /** 返回游客星级 */
  public int getStarlevel() {
    return Starlevel;
  }

  /** 返回游客ID */
  public int getVisitor() {
    return Visitor;
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
