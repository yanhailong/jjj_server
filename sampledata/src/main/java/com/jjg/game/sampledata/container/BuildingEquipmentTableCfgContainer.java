package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.BuildingEquipmentTableCfg;

/**
 * BuildingEquipmentTable.xlsx配置管理容器
 *
 * @excelName BuildingEquipmentTable.xlsx
 * @sheetName BuildingEquipmentTable
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BuildingEquipmentTableCfgContainer extends BaseCfgContainer<BuildingEquipmentTableCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public BuildingEquipmentTableCfgContainer getNewContainer(){
    return new BuildingEquipmentTableCfgContainer();
  }

  public BuildingEquipmentTableCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("BuildingEquipmentTable.xlsx");
    return excelNameList;
  }

  @Override
  protected BuildingEquipmentTableCfg createNewBean() {
    return new BuildingEquipmentTableCfg();
  }
}
