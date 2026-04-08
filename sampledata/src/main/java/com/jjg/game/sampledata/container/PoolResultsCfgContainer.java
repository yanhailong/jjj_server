package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.PoolResultsCfg;

/**
 * poolResults.xlsx配置管理容器
 *
 * @excelName poolResults.xlsx
 * @sheetName poolResults
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class PoolResultsCfgContainer extends BaseCfgContainer<PoolResultsCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public PoolResultsCfgContainer getNewContainer(){
    return new PoolResultsCfgContainer();
  }

  public PoolResultsCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("poolResults.xlsx");
    return excelNameList;
  }

  @Override
  protected PoolResultsCfg createNewBean() {
    return new PoolResultsCfg();
  }
}
