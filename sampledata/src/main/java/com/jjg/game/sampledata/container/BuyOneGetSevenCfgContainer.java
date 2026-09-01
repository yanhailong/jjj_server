package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.BuyOneGetSevenCfg;

/**
 * BuyOneGetSeven.xlsx配置管理容器
 *
 * @excelName BuyOneGetSeven.xlsx
 * @sheetName BuyOneGetSeven
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BuyOneGetSevenCfgContainer extends BaseCfgContainer<BuyOneGetSevenCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public BuyOneGetSevenCfgContainer getNewContainer(){
    return new BuyOneGetSevenCfgContainer();
  }

  public BuyOneGetSevenCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("BuyOneGetSeven.xlsx");
    return excelNameList;
  }

  @Override
  protected BuyOneGetSevenCfg createNewBean() {
    return new BuyOneGetSevenCfg();
  }
}
