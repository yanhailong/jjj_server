package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.DropDetailedCfg;

/**
 * dropDetailed.xlsx配置管理容器
 *
 * @excelName dropDetailed.xlsx
 * @sheetName dropDetailed
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DropDetailedCfgContainer extends BaseCfgContainer<DropDetailedCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public DropDetailedCfgContainer getNewContainer(){
    return new DropDetailedCfgContainer();
  }

  public DropDetailedCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("dropDetailed.xlsx");
    return excelNameList;
  }

  @Override
  protected DropDetailedCfg createNewBean() {
    return new DropDetailedCfg();
  }
}
