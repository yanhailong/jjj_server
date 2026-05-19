package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.PropCfg;

/**
 * prop.xlsx配置管理容器
 *
 * @excelName prop.xlsx
 * @sheetName prop
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PropCfgContainer extends BaseCfgContainer<PropCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public PropCfgContainer getNewContainer(){
    return new PropCfgContainer();
  }

  public PropCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("prop.xlsx");
    return excelNameList;
  }

  @Override
  protected PropCfg createNewBean() {
    return new PropCfg();
  }
}
