package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.VisitorPoolCfg;

/**
 * VisitorPool.xlsx配置管理容器
 *
 * @excelName VisitorPool.xlsx
 * @sheetName VisitorPool
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class VisitorPoolCfgContainer extends BaseCfgContainer<VisitorPoolCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public VisitorPoolCfgContainer getNewContainer(){
    return new VisitorPoolCfgContainer();
  }

  public VisitorPoolCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("VisitorPool.xlsx");
    return excelNameList;
  }

  @Override
  protected VisitorPoolCfg createNewBean() {
    return new VisitorPoolCfg();
  }
}
