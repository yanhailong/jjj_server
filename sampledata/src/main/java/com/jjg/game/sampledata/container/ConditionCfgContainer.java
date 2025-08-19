package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.ConditionCfg;

/**
 * condition.xlsx配置管理容器
 *
 * @excelName condition.xlsx
 * @sheetName condition
 * @author auto_generator
 * @date 2025年08月19日 11:30:38
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ConditionCfgContainer extends BaseCfgContainer<ConditionCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public ConditionCfgContainer getNewContainer(){
    return new ConditionCfgContainer();
  }

  public ConditionCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("condition.xlsx");
    return excelNameList;
  }

  @Override
  protected ConditionCfg createNewBean() {
    return new ConditionCfg();
  }
}
