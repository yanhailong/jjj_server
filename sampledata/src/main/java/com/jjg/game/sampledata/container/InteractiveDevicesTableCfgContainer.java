package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.InteractiveDevicesTableCfg;

/**
 * InteractiveDevicesTable.xlsx配置管理容器
 *
 * @excelName InteractiveDevicesTable.xlsx
 * @sheetName InteractiveDevicesTable
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class InteractiveDevicesTableCfgContainer extends BaseCfgContainer<InteractiveDevicesTableCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public InteractiveDevicesTableCfgContainer getNewContainer(){
    return new InteractiveDevicesTableCfgContainer();
  }

  public InteractiveDevicesTableCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("InteractiveDevicesTable.xlsx");
    return excelNameList;
  }

  @Override
  protected InteractiveDevicesTableCfg createNewBean() {
    return new InteractiveDevicesTableCfg();
  }
}
