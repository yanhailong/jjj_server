package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.CashcowCfg;

/**
 * CashCow.xlsx配置管理容器
 *
 * @excelName CashCow.xlsx
 * @sheetName cashcow
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class CashcowCfgContainer extends BaseCfgContainer<CashcowCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public CashcowCfgContainer getNewContainer(){
    return new CashcowCfgContainer();
  }

  public CashcowCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("CashCow.xlsx");
    return excelNameList;
  }

  @Override
  protected CashcowCfg createNewBean() {
    return new CashcowCfg();
  }
}
