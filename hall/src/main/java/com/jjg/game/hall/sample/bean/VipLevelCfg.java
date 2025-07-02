package com.jjg.game.hall.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName vipLevel.xlsx
 * @sheetName VipLevel
 * @author Auto.Generator
 * @date 2025年07月18日 15:03:10
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class VipLevelCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "vipLevel.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "VipLevel";

  /** 升级所需经验 */
  protected int levelUpExp;
  /** 额外流水系数 */
  protected int prop;

  /** 返回升级所需经验 */
  public int getLevelUpExp() {
    return levelUpExp;
  }

  /** 返回额外流水系数 */
  public int getProp() {
    return prop;
  }
}
