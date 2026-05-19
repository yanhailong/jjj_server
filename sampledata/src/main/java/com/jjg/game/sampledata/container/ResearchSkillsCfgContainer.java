package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.ResearchSkillsCfg;

/**
 * ResearchSkills.xlsx配置管理容器
 *
 * @excelName ResearchSkills.xlsx
 * @sheetName ResearchSkills
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ResearchSkillsCfgContainer extends BaseCfgContainer<ResearchSkillsCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public ResearchSkillsCfgContainer getNewContainer(){
    return new ResearchSkillsCfgContainer();
  }

  public ResearchSkillsCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("ResearchSkills.xlsx");
    return excelNameList;
  }

  @Override
  protected ResearchSkillsCfg createNewBean() {
    return new ResearchSkillsCfg();
  }
}
