package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.VisitorGenPaidCfg;

/**
 * VisitorGenPaid.xlsx配置管理容器
 *
 * @excelName VisitorGenPaid.xlsx
 * @sheetName VisitorGenPaid
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class VisitorGenPaidCfgContainer extends BaseCfgContainer<VisitorGenPaidCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public VisitorGenPaidCfgContainer getNewContainer(){
    return new VisitorGenPaidCfgContainer();
  }

  public VisitorGenPaidCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("VisitorGenPaid.xlsx");
    return excelNameList;
  }

  @Override
  protected VisitorGenPaidCfg createNewBean() {
    return new VisitorGenPaidCfg();
  }
}
