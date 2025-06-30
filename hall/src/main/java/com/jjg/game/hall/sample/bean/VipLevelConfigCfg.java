package com.jjg.game.hall.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName vipLevel.xlsx
 * @sheetName VipLevelConfig
 * @author Auto.Generator
 * @date 2025年06月27日 16:42:37
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class VipLevelConfigCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "vipLevel.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "VipLevelConfig";

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
