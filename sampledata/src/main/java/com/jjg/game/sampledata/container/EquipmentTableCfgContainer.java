package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.EquipmentTableCfg;

/**
 * EquipmentTable.xlsx配置管理容器
 *
 * @excelName EquipmentTable.xlsx
 * @sheetName EquipmentTable
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class EquipmentTableCfgContainer extends BaseCfgContainer<EquipmentTableCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public EquipmentTableCfgContainer getNewContainer(){
    return new EquipmentTableCfgContainer();
  }

  public EquipmentTableCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("EquipmentTable.xlsx");
    return excelNameList;
  }

  @Override
  protected EquipmentTableCfg createNewBean() {
    return new EquipmentTableCfg();
  }
}
