package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.DropTypeCfg;

/**
 * dropType.xlsx配置管理容器
 *
 * @excelName dropType.xlsx
 * @sheetName dropType
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DropTypeCfgContainer extends BaseCfgContainer<DropTypeCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public DropTypeCfgContainer getNewContainer(){
    return new DropTypeCfgContainer();
  }

  public DropTypeCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("dropType.xlsx");
    return excelNameList;
  }

  @Override
  protected DropTypeCfg createNewBean() {
    return new DropTypeCfg();
  }
}
