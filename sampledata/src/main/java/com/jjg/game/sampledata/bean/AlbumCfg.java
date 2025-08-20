package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName album.xlsx
 * @sheetName album
 * @author Auto.Generator
 * @date 2025年08月20日 13:34:26
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class AlbumCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "album.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "album";

  /** 图鉴组ID */
  protected int group;
  /** 重复激活后分解获得道具 */
  protected Map<Integer,Long> obtainedDecomposition;
  /** 序列ID */
  protected int sequence;

  /** 返回图鉴组ID */
  public int getGroup() {
    return group;
  }

  /** 返回重复激活后分解获得道具 */
  public Map<Integer,Long> getObtainedDecomposition() {
    return obtainedDecomposition;
  }

  /** 返回序列ID */
  public int getSequence() {
    return sequence;
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
