package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName MiniGame.xlsx
 * @sheetName MiniGame
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class MiniGameCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "MiniGame.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "MiniGame";

  /** 游戏ID */
  protected int gameID;
  /** 多语言表ID */
  protected int nameid;
  /** VIP等级限制 */
  protected int vipLvLimit;

  /** 返回游戏ID */
  public int getGameID() {
    return gameID;
  }

  /** 返回多语言表ID */
  public int getNameid() {
    return nameid;
  }

  /** 返回VIP等级限制 */
  public int getVipLvLimit() {
    return vipLvLimit;
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
