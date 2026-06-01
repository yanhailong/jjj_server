package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.EmployeeSkillConfigCfg;

/**
 * EmployeeSkillConfig.xlsx配置管理容器
 *
 * @excelName EmployeeSkillConfig.xlsx
 * @sheetName EmployeeSkillConfig
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class EmployeeSkillConfigCfgContainer extends BaseCfgContainer<EmployeeSkillConfigCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public EmployeeSkillConfigCfgContainer getNewContainer(){
    return new EmployeeSkillConfigCfgContainer();
  }

  public EmployeeSkillConfigCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("EmployeeSkillConfig.xlsx");
    return excelNameList;
  }

  @Override
  protected EmployeeSkillConfigCfg createNewBean() {
    return new EmployeeSkillConfigCfg();
  }
}
