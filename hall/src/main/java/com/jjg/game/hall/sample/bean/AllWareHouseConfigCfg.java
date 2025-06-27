package com.jjg.game.hall.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName allWareHouse.xlsx
 * @sheetName AllWareHouseConfig
 * @author Auto.Generator
 * @date 2025年06月27日 16:42:37
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class AllWareHouseConfigCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "allWareHouse.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "AllWareHouseConfig";

  /** 水池初始值 */
  protected long basicWarehouse;
  /** 游戏类型 */
  protected int gameType;
  /** 水池大奖虚拟金额 */
  protected int grandPrize;
  /** 说明 */
  protected String name;
  /** 进入条件_最低金额 */
  protected int require_amount;
  /** 进入条件_VIP等级 */
  protected int require_viplevel;
  /** 场次id */
  protected int wareId;

  /** 返回水池初始值 */
  public long getBasicWarehouse() {
    return basicWarehouse;
  }

  /** 返回游戏类型 */
  public int getGameType() {
    return gameType;
  }

  /** 返回水池大奖虚拟金额 */
  public int getGrandPrize() {
    return grandPrize;
  }

  /** 返回说明 */
  public String getName() {
    return name;
  }

  /** 返回进入条件_最低金额 */
  public int getRequire_amount() {
    return require_amount;
  }

  /** 返回进入条件_VIP等级 */
  public int getRequire_viplevel() {
    return require_viplevel;
  }

  /** 返回场次id */
  public int getWareId() {
    return wareId;
  }
}
