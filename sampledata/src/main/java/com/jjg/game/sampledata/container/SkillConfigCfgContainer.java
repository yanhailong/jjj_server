package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.SkillConfigCfg;

/**
 * SkillConfig.xlsx配置管理容器
 *
 * @excelName SkillConfig.xlsx
 * @sheetName SkillConfig
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SkillConfigCfgContainer extends BaseCfgContainer<SkillConfigCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public SkillConfigCfgContainer getNewContainer(){
    return new SkillConfigCfgContainer();
  }

  public SkillConfigCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("SkillConfig.xlsx");
    return excelNameList;
  }

  @Override
  protected SkillConfigCfg createNewBean() {
    return new SkillConfigCfg();
  }
}
