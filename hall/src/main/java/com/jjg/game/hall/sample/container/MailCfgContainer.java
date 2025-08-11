package com.jjg.game.hall.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.hall.sample.bean.MailCfg;

/**
 * mail.xlsx配置管理容器
 *
 * @excelName mail.xlsx
 * @sheetName mail
 * @author auto_generator
 * @date 2025年08月08日 13:44:57
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class MailCfgContainer extends BaseCfgContainer<MailCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public MailCfgContainer getNewContainer(){
    return new MailCfgContainer();
  }

  public MailCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("mail.xlsx");
    return excelNameList;
  }

  @Override
  protected MailCfg createNewBean() {
    return new MailCfg();
  }
}
