package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.SpecialModeCfg;

/**
 * SpecialMode.xlsx配置管理容器
 *
 * @excelName SpecialMode.xlsx
 * @sheetName SpecialMode
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class SpecialModeCfgContainer extends BaseCfgContainer<SpecialModeCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public SpecialModeCfgContainer getNewContainer(){
    return new SpecialModeCfgContainer();
  }

  public SpecialModeCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("SpecialMode.xlsx");
    return excelNameList;
  }

  @Override
  protected SpecialModeCfg createNewBean() {
    return new SpecialModeCfg();
  }
}
