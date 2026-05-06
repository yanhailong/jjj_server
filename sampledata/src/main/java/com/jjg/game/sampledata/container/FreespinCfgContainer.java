package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.FreespinCfg;

/**
 * Freespin.xlsx配置管理容器
 *
 * @excelName Freespin.xlsx
 * @sheetName freespin
 * @author auto_generator
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class FreespinCfgContainer extends BaseCfgContainer<FreespinCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public FreespinCfgContainer getNewContainer(){
    return new FreespinCfgContainer();
  }

  public FreespinCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("Freespin.xlsx");
    return excelNameList;
  }

  @Override
  protected FreespinCfg createNewBean() {
    return new FreespinCfg();
  }
}
