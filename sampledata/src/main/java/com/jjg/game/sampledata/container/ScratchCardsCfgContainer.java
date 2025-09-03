package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.ScratchCardsCfg;

/**
 * ScratchCards.xlsx配置管理容器
 *
 * @excelName ScratchCards.xlsx
 * @sheetName ScratchCards
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ScratchCardsCfgContainer extends BaseCfgContainer<ScratchCardsCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public ScratchCardsCfgContainer getNewContainer(){
    return new ScratchCardsCfgContainer();
  }

  public ScratchCardsCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("ScratchCards.xlsx");
    return excelNameList;
  }

  @Override
  protected ScratchCardsCfg createNewBean() {
    return new ScratchCardsCfg();
  }
}
