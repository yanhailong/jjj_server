package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.EmployeeProfileCfg;

/**
 * EmployeeProfile.xlsx配置管理容器
 *
 * @excelName EmployeeProfile.xlsx
 * @sheetName EmployeeProfile
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class EmployeeProfileCfgContainer extends BaseCfgContainer<EmployeeProfileCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public EmployeeProfileCfgContainer getNewContainer(){
    return new EmployeeProfileCfgContainer();
  }

  public EmployeeProfileCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("EmployeeProfile.xlsx");
    return excelNameList;
  }

  @Override
  protected EmployeeProfileCfg createNewBean() {
    return new EmployeeProfileCfg();
  }
}
