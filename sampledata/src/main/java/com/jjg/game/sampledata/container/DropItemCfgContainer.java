package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.DropItemCfg;

/**
 * dropItem.xlsx配置管理容器
 *
 * @excelName dropItem.xlsx
 * @sheetName dropItem
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DropItemCfgContainer extends BaseCfgContainer<DropItemCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public DropItemCfgContainer getNewContainer(){
    return new DropItemCfgContainer();
  }

  public DropItemCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("dropItem.xlsx");
    return excelNameList;
  }

  @Override
  protected DropItemCfg createNewBean() {
    return new DropItemCfg();
  }
}
