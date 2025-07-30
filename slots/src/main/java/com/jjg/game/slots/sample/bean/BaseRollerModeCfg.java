package com.jjg.game.slots.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName BaseRollerMode.xlsx
 * @sheetName BaseRollerMode
 * @author Auto.Generator
 * @date 2025年07月30日 10:16:30
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BaseRollerModeCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "BaseRollerMode.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "BaseRollerMode";

  /** 游戏ID */
  protected int gameType;
  /** 滚轴使用 */
  protected Map<Integer,List<Integer>> rollerMode;
  /** 特殊滚轴id */
  protected List<Integer> specialRollerId;

  /** 返回游戏ID */
  public int getGameType() {
    return gameType;
  }

  /** 返回滚轴使用 */
  public Map<Integer,List<Integer>> getRollerMode() {
    return rollerMode;
  }

  /** 返回特殊滚轴id */
  public List<Integer> getSpecialRollerId() {
    return specialRollerId;
  }
}
