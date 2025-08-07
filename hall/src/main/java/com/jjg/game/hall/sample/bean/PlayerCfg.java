package com.jjg.game.hall.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName player.xlsx
 * @sheetName player
 * @author Auto.Generator
 * @date 2025年08月06日 20:26:26
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PlayerCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "player.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "player";

  /** 资源名称 */
  protected String name;
  /** 美术资源名称与路径 */
  protected String resourcePath;
  /** 类型 */
  protected int resourceType;
  /** 资源id */
  protected int rid;

  /** 返回资源名称 */
  public String getName() {
    return name;
  }

  /** 返回美术资源名称与路径 */
  public String getResourcePath() {
    return resourcePath;
  }

  /** 返回类型 */
  public int getResourceType() {
    return resourceType;
  }

  /** 返回资源id */
  public int getRid() {
    return rid;
  }
}
