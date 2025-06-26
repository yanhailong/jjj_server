package com.jjg.game.dollarexpress.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.dollarexpress.sample.bean.DollarExpressFreeWeightConfigCfg;

/**
 * dollarExpressFreeWeight.xlsx配置管理容器
 *
 * @excelName dollarExpressFreeWeight.xlsx
 * @sheetName DollarExpressFreeWeightConfig
 * @author auto_generator
 * @date 2025年06月24日 16:33:11
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DollarExpressFreeWeightConfigCfgContainer extends BaseCfgContainer<DollarExpressFreeWeightConfigCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public DollarExpressFreeWeightConfigCfgContainer getNewContainer(){
    return new DollarExpressFreeWeightConfigCfgContainer();
  }

  public DollarExpressFreeWeightConfigCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("dollarExpressFreeWeight.xlsx");
    return excelNameList;
  }

  @Override
  protected DollarExpressFreeWeightConfigCfg createNewBean() {
    return new DollarExpressFreeWeightConfigCfg();
  }
}
