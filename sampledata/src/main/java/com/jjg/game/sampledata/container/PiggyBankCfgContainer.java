package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.PiggyBankCfg;

/**
 * PiggyBank.xlsx配置管理容器
 *
 * @excelName PiggyBank.xlsx
 * @sheetName PiggyBank
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PiggyBankCfgContainer extends BaseCfgContainer<PiggyBankCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public PiggyBankCfgContainer getNewContainer(){
    return new PiggyBankCfgContainer();
  }

  public PiggyBankCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("PiggyBank.xlsx");
    return excelNameList;
  }

  @Override
  protected PiggyBankCfg createNewBean() {
    return new PiggyBankCfg();
  }
}
