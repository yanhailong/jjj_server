package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.EmployeeStarCfg;

/**
 * EmployeeStar.xlsx配置管理容器
 *
 * @excelName EmployeeStar.xlsx
 * @sheetName EmployeeStar
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class EmployeeStarCfgContainer extends BaseCfgContainer<EmployeeStarCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public EmployeeStarCfgContainer getNewContainer(){
    return new EmployeeStarCfgContainer();
  }

  public EmployeeStarCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("EmployeeStar.xlsx");
    return excelNameList;
  }

  @Override
  protected EmployeeStarCfg createNewBean() {
    return new EmployeeStarCfg();
  }
}
