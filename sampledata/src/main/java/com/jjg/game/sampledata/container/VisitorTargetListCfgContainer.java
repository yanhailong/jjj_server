package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.VisitorTargetListCfg;

/**
 * VisitorTargetList.xlsx配置管理容器
 *
 * @excelName VisitorTargetList.xlsx
 * @sheetName VisitorTargetList
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class VisitorTargetListCfgContainer extends BaseCfgContainer<VisitorTargetListCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public VisitorTargetListCfgContainer getNewContainer(){
    return new VisitorTargetListCfgContainer();
  }

  public VisitorTargetListCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("VisitorTargetList.xlsx");
    return excelNameList;
  }

  @Override
  protected VisitorTargetListCfg createNewBean() {
    return new VisitorTargetListCfg();
  }
}
