package com.jjg.game.slots.game.dollarexpress.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName dollarExpressWareHouse.xlsx
 * @sheetName DollarExpressWareHouse
 * @author Auto.Generator
 * @date 2025年06月27日 09:57:50
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DollarExpressWareHouseCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "dollarExpressWareHouse.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "DollarExpressWareHouse";

  /** 水池初始值 */
  protected long basicWarehouse;
  /** 押分列表 */
  protected List<Integer> betList;
  /** 默认押分 */
  protected int defaultBet;
  /** 底注倍数 */
  protected int multiplier;

  /** 返回水池初始值 */
  public long getBasicWarehouse() {
    return basicWarehouse;
  }

  /** 返回押分列表 */
  public List<Integer> getBetList() {
    return betList;
  }

  /** 返回默认押分 */
  public int getDefaultBet() {
    return defaultBet;
  }

  /** 返回底注倍数 */
  public int getMultiplier() {
    return multiplier;
  }
}
