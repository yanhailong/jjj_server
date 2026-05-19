package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.VisitorBehaviorCfg;

/**
 * VisitorBehavior.xlsx配置管理容器
 *
 * @excelName VisitorBehavior.xlsx
 * @sheetName VisitorBehavior
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class VisitorBehaviorCfgContainer extends BaseCfgContainer<VisitorBehaviorCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public VisitorBehaviorCfgContainer getNewContainer(){
    return new VisitorBehaviorCfgContainer();
  }

  public VisitorBehaviorCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("VisitorBehavior.xlsx");
    return excelNameList;
  }

  @Override
  protected VisitorBehaviorCfg createNewBean() {
    return new VisitorBehaviorCfg();
  }
}
