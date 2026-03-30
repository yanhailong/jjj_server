package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.AirRaidCfg;

/**
 * AirRaid.xlsx配置管理容器
 *
 * @excelName AirRaid.xlsx
 * @sheetName AirRaid
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class AirRaidCfgContainer extends BaseCfgContainer<AirRaidCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public AirRaidCfgContainer getNewContainer(){
    return new AirRaidCfgContainer();
  }

  public AirRaidCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("AirRaid.xlsx");
    return excelNameList;
  }

  @Override
  protected AirRaidCfg createNewBean() {
    return new AirRaidCfg();
  }
}
