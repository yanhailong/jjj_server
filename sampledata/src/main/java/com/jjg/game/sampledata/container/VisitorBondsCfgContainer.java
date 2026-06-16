package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.VisitorBondsCfg;

/**
 * VisitorBonds.xlsx配置管理容器
 *
 * @excelName VisitorBonds.xlsx
 * @sheetName VisitorBonds
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class VisitorBondsCfgContainer extends BaseCfgContainer<VisitorBondsCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public VisitorBondsCfgContainer getNewContainer(){
    return new VisitorBondsCfgContainer();
  }

  public VisitorBondsCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("VisitorBonds.xlsx");
    return excelNameList;
  }

  @Override
  protected VisitorBondsCfg createNewBean() {
    return new VisitorBondsCfg();
  }
}
