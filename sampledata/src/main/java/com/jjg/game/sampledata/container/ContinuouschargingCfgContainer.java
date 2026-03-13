package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.ContinuouschargingCfg;

/**
 * Continuouscharging.xlsx配置管理容器
 *
 * @excelName Continuouscharging.xlsx
 * @sheetName Continuouscharging
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ContinuouschargingCfgContainer extends BaseCfgContainer<ContinuouschargingCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public ContinuouschargingCfgContainer getNewContainer(){
    return new ContinuouschargingCfgContainer();
  }

  public ContinuouschargingCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("Continuouscharging.xlsx");
    return excelNameList;
  }

  @Override
  protected ContinuouschargingCfg createNewBean() {
    return new ContinuouschargingCfg();
  }
}
