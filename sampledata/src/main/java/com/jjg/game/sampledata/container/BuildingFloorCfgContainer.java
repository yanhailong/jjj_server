package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.BuildingFloorCfg;

/**
 * BuildingFloor.xlsx配置管理容器
 *
 * @excelName BuildingFloor.xlsx
 * @sheetName BuildingFloor
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BuildingFloorCfgContainer extends BaseCfgContainer<BuildingFloorCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public BuildingFloorCfgContainer getNewContainer(){
    return new BuildingFloorCfgContainer();
  }

  public BuildingFloorCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("BuildingFloor.xlsx");
    return excelNameList;
  }

  @Override
  protected BuildingFloorCfg createNewBean() {
    return new BuildingFloorCfg();
  }
}
