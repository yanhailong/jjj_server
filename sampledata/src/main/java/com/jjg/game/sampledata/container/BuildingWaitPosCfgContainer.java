package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.BuildingWaitPosCfg;

/**
 * BuildingWaitPos.xlsx配置管理容器
 *
 * @excelName BuildingWaitPos.xlsx
 * @sheetName BuildingWaitPos
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class BuildingWaitPosCfgContainer extends BaseCfgContainer<BuildingWaitPosCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public BuildingWaitPosCfgContainer getNewContainer(){
    return new BuildingWaitPosCfgContainer();
  }

  public BuildingWaitPosCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("BuildingWaitPos.xlsx");
    return excelNameList;
  }

  @Override
  protected BuildingWaitPosCfg createNewBean() {
    return new BuildingWaitPosCfg();
  }
}
