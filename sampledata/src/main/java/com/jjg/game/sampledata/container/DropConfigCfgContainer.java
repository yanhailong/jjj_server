package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.DropConfigCfg;

/**
 * dropConfig.xlsx配置管理容器
 *
 * @excelName dropConfig.xlsx
 * @sheetName dropConfig
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DropConfigCfgContainer extends BaseCfgContainer<DropConfigCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public DropConfigCfgContainer getNewContainer(){
    return new DropConfigCfgContainer();
  }

  public DropConfigCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("dropConfig.xlsx");
    return excelNameList;
  }

  @Override
  protected DropConfigCfg createNewBean() {
    return new DropConfigCfg();
  }
}
