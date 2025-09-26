package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName PointsAwardTask.xlsx
 * @sheetName PointsAwardTask
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PointsAwardTaskCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "PointsAwardTask.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "PointsAwardTask";

  /** 任务描述 */
  protected String des;
  /** 奖励 */
  protected List<Integer> getitem;
  /** 参数3(I值) */
  protected int ikey;
  /** 参数1(N值) */
  protected int nkey;
  /** 任务类型 */
  protected int turntableType;
  /** 参数2(X值) */
  protected int xkey;

  /** 返回任务描述 */
  public String getDes() {
    return des;
  }

  /** 返回奖励 */
  public List<Integer> getGetitem() {
    return getitem;
  }

  /** 返回参数3(I值) */
  public int getIkey() {
    return ikey;
  }

  /** 返回参数1(N值) */
  public int getNkey() {
    return nkey;
  }

  /** 返回任务类型 */
  public int getTurntableType() {
    return turntableType;
  }

  /** 返回参数2(X值) */
  public int getXkey() {
    return xkey;
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
