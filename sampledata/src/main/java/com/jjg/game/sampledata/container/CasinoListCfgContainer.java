package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.CasinoListCfg;

/**
 * CasinoList.xlsx配置管理容器
 *
 * @excelName CasinoList.xlsx
 * @sheetName CasinoList
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class CasinoListCfgContainer extends BaseCfgContainer<CasinoListCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public CasinoListCfgContainer getNewContainer(){
    return new CasinoListCfgContainer();
  }

  public CasinoListCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("CasinoList.xlsx");
    return excelNameList;
  }

  @Override
  protected CasinoListCfg createNewBean() {
    return new CasinoListCfg();
  }
}
