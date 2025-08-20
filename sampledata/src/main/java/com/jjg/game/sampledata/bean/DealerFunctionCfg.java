package com.jjg.game.sampledata.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName DealerFunction.xlsx
 * @sheetName DealerFunction
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DealerFunctionCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "DealerFunction.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "DealerFunction";

  /** 增加收益BUFF */
  protected int buffid;
  /** 荷官描述多语言ID */
  protected int describe;
  /** 持续时间/秒 */
  protected int duration;
  /** 聘请费用 */
  protected List<Integer> hiringExpenses;
  /** 荷官名字多语言ID */
  protected int name;
  /** 何官品级 */
  protected int quality;

  /** 返回增加收益BUFF */
  public int getBuffid() {
    return buffid;
  }

  /** 返回荷官描述多语言ID */
  public int getDescribe() {
    return describe;
  }

  /** 返回持续时间/秒 */
  public int getDuration() {
    return duration;
  }

  /** 返回聘请费用 */
  public List<Integer> getHiringExpenses() {
    return hiringExpenses;
  }

  /** 返回荷官名字多语言ID */
  public int getName() {
    return name;
  }

  /** 返回何官品级 */
  public int getQuality() {
    return quality;
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
