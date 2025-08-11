package com.jjg.game.hall.sample.bean;

import java.util.*;

import javax.annotation.processing.Generated;
/**
 * 配置bean
 *
 * @excelName mail.xlsx
 * @sheetName mail
 * @author Auto.Generator
 * @date 2025年08月08日 13:44:57
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class MailCfg extends BaseCfgBean {

  /** 配置表名 */
  public static final String EXCEL_NAME = "mail.xlsx";
  /** 配置表工作薄名 */
  public static final String SHEET_NAME = "mail";

  /** 发件人 */
  protected String playername;
  /** 邮件内容 */
  protected String text;
  /** 有效时间/秒 */
  protected int time;
  /** 邮件标题 */
  protected String title;

  /** 返回发件人 */
  public String getPlayername() {
    return playername;
  }

  /** 返回邮件内容 */
  public String getText() {
    return text;
  }

  /** 返回有效时间/秒 */
  public int getTime() {
    return time;
  }

  /** 返回邮件标题 */
  public String getTitle() {
    return title;
  }
}
