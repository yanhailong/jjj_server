package com.jjg.game.hall.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName gameList.xlsx
 * @sheetName GameListConfig
 * @author Auto.Generator
 * @date 2025年06月27日 16:42:37
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class GameListConfigCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "gameList.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "GameListConfig";

  /** 名称 */
  protected String name;
  /** 状态  0.开启  1.维护  2.关闭 */
  protected int status;

  /** 返回名称 */
  public String getName() {
    return name;
  }

  /** 返回状态  0.开启  1.维护  2.关闭 */
  public int getStatus() {
    return status;
  }
}
