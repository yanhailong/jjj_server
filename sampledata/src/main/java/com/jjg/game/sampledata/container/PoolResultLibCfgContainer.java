package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.PoolResultLibCfg;

/**
 * PoolResultLib.xlsx配置管理容器
 *
 * @excelName PoolResultLib.xlsx
 * @sheetName PoolResultLib
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PoolResultLibCfgContainer extends BaseCfgContainer<PoolResultLibCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public PoolResultLibCfgContainer getNewContainer(){
    return new PoolResultLibCfgContainer();
  }

  public PoolResultLibCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("PoolResultLib.xlsx");
    return excelNameList;
  }

  @Override
  protected PoolResultLibCfg createNewBean() {
    return new PoolResultLibCfg();
  }
}
