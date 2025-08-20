package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName avatar.xlsx
 * @sheetName avatar
 * @author Auto.Generator
 * @date 2025年08月20日 13:34:26
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class AvatarCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "avatar.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "avatar";

  /** 资源描述 */
  protected int Dec;
  /** 资源名称 */
  protected int name;
  /** 美术资源名称与路径 */
  protected String resourcePath;
  /** 类型 */
  protected int resourceType;

  /** 返回资源描述 */
  public int getDec() {
    return Dec;
  }

  /** 返回资源名称 */
  public int getName() {
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

  @Override
  public boolean equals(Object o) {
    return super.equals(o);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
