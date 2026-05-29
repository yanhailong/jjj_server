package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.EmployeeLevelCfg;

/**
 * EmployeeLevel.xlsx配置管理容器
 *
 * @excelName EmployeeLevel.xlsx
 * @sheetName EmployeeLevel
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class EmployeeLevelCfgContainer extends BaseCfgContainer<EmployeeLevelCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public EmployeeLevelCfgContainer getNewContainer(){
    return new EmployeeLevelCfgContainer();
  }

  public EmployeeLevelCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("EmployeeLevel.xlsx");
    return excelNameList;
  }

  @Override
  protected EmployeeLevelCfg createNewBean() {
    return new EmployeeLevelCfg();
  }
}
