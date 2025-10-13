package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName jumpData.xlsx
 * @sheetName jumpData
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class JumpDataCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "jumpData.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "jumpData";

  /** 仅填写游戏ID */
  protected int gameId;
  /** 类型 */
  protected int jumpTypeId;
  /** 仅填写功能\活动界面名称 */
  protected String uiName;

  /** 返回仅填写游戏ID */
  public int getGameId() {
    return gameId;
  }

  /** 返回类型 */
  public int getJumpTypeId() {
    return jumpTypeId;
  }

  /** 返回仅填写功能\活动界面名称 */
  public String getUiName() {
    return uiName;
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
