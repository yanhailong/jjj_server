package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName SeasonStart.xlsx
 * @sheetName SeasonStart
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SeasonStartCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "SeasonStart.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "SeasonStart";

  /** 赛季开启的游戏 */
  protected int AvailableGames;
  /** 下注范围 */
  protected List<Integer> BetRange;
  /** 特色玩法多语言 */
  protected int FeatureName;
  /** 游戏的RTP */
  protected int GameRTP;
  /** 开启的宝石孔数 */
  protected int GemCount;
  /** 循环的序列 */
  protected int LoopSequence;
  /** 游戏的最大倍率 */
  protected int MaxMultiplier;
  /** 开启的小游戏 */
  protected List<Integer> Openminigame;
  /** 赛季持续时间 */
  protected int SeasonDuration;
  /** 赛季名称多语言 */
  protected int SeasonName;

  /** 返回赛季开启的游戏 */
  public int getAvailableGames() {
    return AvailableGames;
  }

  /** 返回下注范围 */
  public List<Integer> getBetRange() {
    return BetRange;
  }

  /** 返回特色玩法多语言 */
  public int getFeatureName() {
    return FeatureName;
  }

  /** 返回游戏的RTP */
  public int getGameRTP() {
    return GameRTP;
  }

  /** 返回开启的宝石孔数 */
  public int getGemCount() {
    return GemCount;
  }

  /** 返回循环的序列 */
  public int getLoopSequence() {
    return LoopSequence;
  }

  /** 返回游戏的最大倍率 */
  public int getMaxMultiplier() {
    return MaxMultiplier;
  }

  /** 返回开启的小游戏 */
  public List<Integer> getOpenminigame() {
    return Openminigame;
  }

  /** 返回赛季持续时间 */
  public int getSeasonDuration() {
    return SeasonDuration;
  }

  /** 返回赛季名称多语言 */
  public int getSeasonName() {
    return SeasonName;
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
