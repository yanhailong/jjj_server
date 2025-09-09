package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.CashcowrewardsCfg;

/**
 * CashCowRewards.xlsx配置管理容器
 *
 * @excelName CashCowRewards.xlsx
 * @sheetName cashcowrewards
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class CashcowrewardsCfgContainer extends BaseCfgContainer<CashcowrewardsCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public CashcowrewardsCfgContainer getNewContainer(){
    return new CashcowrewardsCfgContainer();
  }

  public CashcowrewardsCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("CashCowRewards.xlsx");
    return excelNameList;
  }

  @Override
  protected CashcowrewardsCfg createNewBean() {
    return new CashcowrewardsCfg();
  }
}
