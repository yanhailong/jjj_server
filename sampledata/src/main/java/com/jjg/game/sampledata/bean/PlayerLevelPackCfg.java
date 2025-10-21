package com.jjg.game.sampledata.bean;

import java.util.*;
import java.math.BigDecimal;




import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName PlayerLevelPack.xlsx
 * @sheetName PlayerLevelPack
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PlayerLevelPackCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "PlayerLevelPack.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "PlayerLevelPack";

  /** 等级奖励 */
  protected Map<Integer,Long> levelRewards;
  /** 购买金额 */
  protected BigDecimal pay;
  /** 等级 */
  protected int playerlevel;
  /** 有效时长(分钟) */
  protected int time;

  /** 返回等级奖励 */
  public Map<Integer,Long> getLevelRewards() {
    return levelRewards;
  }

  /** 返回购买金额 */
  public BigDecimal getPay() {
    return pay;
  }

  /** 返回等级 */
  public int getPlayerlevel() {
    return playerlevel;
  }

  /** 返回有效时长(分钟) */
  public int getTime() {
    return time;
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
