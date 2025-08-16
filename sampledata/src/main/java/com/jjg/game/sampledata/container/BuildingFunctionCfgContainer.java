package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.BuildingFunctionCfg;

/**
 * BuildingFunction.xlsx配置管理容器
 *
 * @excelName BuildingFunction.xlsx
 * @sheetName BuildingFunction
 * @author auto_generator
 * @date 2025年08月16日 15:49:31
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BuildingFunctionCfgContainer extends BaseCfgContainer<BuildingFunctionCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public BuildingFunctionCfgContainer getNewContainer(){
    return new BuildingFunctionCfgContainer();
  }

  public BuildingFunctionCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("BuildingFunction.xlsx");
    return excelNameList;
  }

  @Override
  protected BuildingFunctionCfg createNewBean() {
    return new BuildingFunctionCfg();
  }
}
