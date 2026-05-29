package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.BuildingUpgradeTableCfg;

/**
 * BuildingUpgradeTable.xlsx配置管理容器
 *
 * @excelName BuildingUpgradeTable.xlsx
 * @sheetName BuildingUpgradeTable
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BuildingUpgradeTableCfgContainer extends BaseCfgContainer<BuildingUpgradeTableCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public BuildingUpgradeTableCfgContainer getNewContainer(){
    return new BuildingUpgradeTableCfgContainer();
  }

  public BuildingUpgradeTableCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("BuildingUpgradeTable.xlsx");
    return excelNameList;
  }

  @Override
  protected BuildingUpgradeTableCfg createNewBean() {
    return new BuildingUpgradeTableCfg();
  }
}
