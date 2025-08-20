package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.DropGroupCfg;

/**
 * dropGroup.xlsx配置管理容器
 *
 * @excelName dropGroup.xlsx
 * @sheetName dropGroup
 * @author auto_generator
 * @date 2025年08月20日 13:34:26
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DropGroupCfgContainer extends BaseCfgContainer<DropGroupCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public DropGroupCfgContainer getNewContainer(){
    return new DropGroupCfgContainer();
  }

  public DropGroupCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("dropGroup.xlsx");
    return excelNameList;
  }

  @Override
  protected DropGroupCfg createNewBean() {
    return new DropGroupCfg();
  }
}
