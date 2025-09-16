package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.MGLuckyTreasureCfg;

/**
 * MGLuckyTreasure.xlsx配置管理容器
 *
 * @excelName MGLuckyTreasure.xlsx
 * @sheetName MGLuckyTreasure
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class MGLuckyTreasureCfgContainer extends BaseCfgContainer<MGLuckyTreasureCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public MGLuckyTreasureCfgContainer getNewContainer(){
    return new MGLuckyTreasureCfgContainer();
  }

  public MGLuckyTreasureCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("MGLuckyTreasure.xlsx");
    return excelNameList;
  }

  @Override
  protected MGLuckyTreasureCfg createNewBean() {
    return new MGLuckyTreasureCfg();
  }
}
