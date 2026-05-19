package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.ResearchInstituteCfg;

/**
 * ResearchInstitute.xlsx配置管理容器
 *
 * @excelName ResearchInstitute.xlsx
 * @sheetName ResearchInstitute
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ResearchInstituteCfgContainer extends BaseCfgContainer<ResearchInstituteCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public ResearchInstituteCfgContainer getNewContainer(){
    return new ResearchInstituteCfgContainer();
  }

  public ResearchInstituteCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("ResearchInstitute.xlsx");
    return excelNameList;
  }

  @Override
  protected ResearchInstituteCfg createNewBean() {
    return new ResearchInstituteCfg();
  }
}
