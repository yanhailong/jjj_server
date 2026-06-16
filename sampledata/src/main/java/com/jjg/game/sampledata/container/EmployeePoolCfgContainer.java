package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.EmployeePoolCfg;

/**
 * EmployeePool.xlsx配置管理容器
 *
 * @excelName EmployeePool.xlsx
 * @sheetName EmployeePool
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class EmployeePoolCfgContainer extends BaseCfgContainer<EmployeePoolCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public EmployeePoolCfgContainer getNewContainer(){
    return new EmployeePoolCfgContainer();
  }

  public EmployeePoolCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("EmployeePool.xlsx");
    return excelNameList;
  }

  @Override
  protected EmployeePoolCfg createNewBean() {
    return new EmployeePoolCfg();
  }
}
