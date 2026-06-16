package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.PoolListCfg;

/**
 * PoolList.xlsx配置管理容器
 *
 * @excelName PoolList.xlsx
 * @sheetName PoolList
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PoolListCfgContainer extends BaseCfgContainer<PoolListCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public PoolListCfgContainer getNewContainer(){
    return new PoolListCfgContainer();
  }

  public PoolListCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("PoolList.xlsx");
    return excelNameList;
  }

  @Override
  protected PoolListCfg createNewBean() {
    return new PoolListCfg();
  }
}
