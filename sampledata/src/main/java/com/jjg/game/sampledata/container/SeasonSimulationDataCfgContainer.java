package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.SeasonSimulationDataCfg;

/**
 * SeasonSimulationData.xlsx配置管理容器
 *
 * @excelName SeasonSimulationData.xlsx
 * @sheetName SeasonSimulationData
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SeasonSimulationDataCfgContainer extends BaseCfgContainer<SeasonSimulationDataCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public SeasonSimulationDataCfgContainer getNewContainer(){
    return new SeasonSimulationDataCfgContainer();
  }

  public SeasonSimulationDataCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("SeasonSimulationData.xlsx");
    return excelNameList;
  }

  @Override
  protected SeasonSimulationDataCfg createNewBean() {
    return new SeasonSimulationDataCfg();
  }
}
