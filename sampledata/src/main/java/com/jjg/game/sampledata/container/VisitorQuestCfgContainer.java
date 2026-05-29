package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.VisitorQuestCfg;

/**
 * VisitorQuest.xlsx配置管理容器
 *
 * @excelName VisitorQuest.xlsx
 * @sheetName VisitorQuest
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class VisitorQuestCfgContainer extends BaseCfgContainer<VisitorQuestCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public VisitorQuestCfgContainer getNewContainer(){
    return new VisitorQuestCfgContainer();
  }

  public VisitorQuestCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("VisitorQuest.xlsx");
    return excelNameList;
  }

  @Override
  protected VisitorQuestCfg createNewBean() {
    return new VisitorQuestCfg();
  }
}
