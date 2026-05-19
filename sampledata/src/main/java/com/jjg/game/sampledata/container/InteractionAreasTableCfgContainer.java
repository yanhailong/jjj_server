package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.InteractionAreasTableCfg;

/**
 * InteractionAreasTable.xlsx配置管理容器
 *
 * @excelName InteractionAreasTable.xlsx
 * @sheetName InteractionAreasTable
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class InteractionAreasTableCfgContainer extends BaseCfgContainer<InteractionAreasTableCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public InteractionAreasTableCfgContainer getNewContainer(){
    return new InteractionAreasTableCfgContainer();
  }

  public InteractionAreasTableCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("InteractionAreasTable.xlsx");
    return excelNameList;
  }

  @Override
  protected InteractionAreasTableCfg createNewBean() {
    return new InteractionAreasTableCfg();
  }
}
