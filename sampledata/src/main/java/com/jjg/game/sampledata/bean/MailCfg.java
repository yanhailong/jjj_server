package com.jjg.game.sampledata.bean;

import java.util.*;


import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName mail.xlsx
 * @sheetName mail
 * @author Auto.Generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class MailCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "mail.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "mail";

  /** 发件人 */
  protected int playername;
  /** 邮件内容 */
  protected int text;
  /** 有效时间/秒 */
  protected int time;
  /** 邮件标题 */
  protected int title;

  /** 返回发件人 */
  public int getPlayername() {
    return playername;
  }

  /** 返回邮件内容 */
  public int getText() {
    return text;
  }

  /** 返回有效时间/秒 */
  public int getTime() {
    return time;
  }

  /** 返回邮件标题 */
  public int getTitle() {
    return title;
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
