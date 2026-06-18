package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName Guide.xlsx
 * @sheetName Guide
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class GuideCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "Guide.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "Guide";

  /** 触发条件类型 */
  protected int Condition;
  /** 引导组编号 */
  protected int GuideGroupId;
  /** 是否强制引导 */
  protected int MustBeGuided;
  /** 下个引导ID */
  protected int NextGuideId;
  /** 触发场景 */
  protected String PathName;
  /** 语音文件 */
  protected String SoundFile;
  /** 多语言ID */
  protected int StrDes;
  /** 显示类型 */
  protected int Type;
  /** 条件参数1 */
  protected int param1;

  /** 返回触发条件类型 */
  public int getCondition() {
    return Condition;
  }

  /** 返回引导组编号 */
  public int getGuideGroupId() {
    return GuideGroupId;
  }

  /** 返回是否强制引导 */
  public int getMustBeGuided() {
    return MustBeGuided;
  }

  /** 返回下个引导ID */
  public int getNextGuideId() {
    return NextGuideId;
  }

  /** 返回触发场景 */
  public String getPathName() {
    return PathName;
  }

  /** 返回语音文件 */
  public String getSoundFile() {
    return SoundFile;
  }

  /** 返回多语言ID */
  public int getStrDes() {
    return StrDes;
  }

  /** 返回显示类型 */
  public int getType() {
    return Type;
  }

  /** 返回条件参数1 */
  public int getParam1() {
    return param1;
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
