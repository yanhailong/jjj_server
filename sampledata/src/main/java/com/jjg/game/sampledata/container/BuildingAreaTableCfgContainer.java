package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.BuildingAreaTableCfg;

/**
 * BuildingAreaTable.xlsx配置管理容器
 *
 * @excelName BuildingAreaTable.xlsx
 * @sheetName BuildingAreaTable
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BuildingAreaTableCfgContainer extends BaseCfgContainer<BuildingAreaTableCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public BuildingAreaTableCfgContainer getNewContainer(){
    return new BuildingAreaTableCfgContainer();
  }

  public BuildingAreaTableCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("BuildingAreaTable.xlsx");
    return excelNameList;
  }

  @Override
  protected BuildingAreaTableCfg createNewBean() {
    return new BuildingAreaTableCfg();
  }
}
