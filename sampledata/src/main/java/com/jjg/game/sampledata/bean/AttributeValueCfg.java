package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName AttributeValue.xlsx
 * @sheetName AttributeValue
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class AttributeValueCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "AttributeValue.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "AttributeValue";

  /** 属性值 */
  protected int AttributeValue;

  /** 返回属性值 */
  public int getAttributeValue() {
    return AttributeValue;
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
