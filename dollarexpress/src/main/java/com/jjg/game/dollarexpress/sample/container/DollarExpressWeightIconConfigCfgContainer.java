package com.jjg.game.dollarexpress.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.dollarexpress.sample.bean.DollarExpressWeightIconConfigCfg;

/**
 * dollarExpressWeightIcon.xlsx配置管理容器
 *
 * @excelName dollarExpressWeightIcon.xlsx
 * @sheetName DollarExpressWeightIconConfig
 * @author auto_generator
 * @date 2025年06月24日 16:33:11
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DollarExpressWeightIconConfigCfgContainer extends BaseCfgContainer<DollarExpressWeightIconConfigCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public DollarExpressWeightIconConfigCfgContainer getNewContainer(){
    return new DollarExpressWeightIconConfigCfgContainer();
  }

  public DollarExpressWeightIconConfigCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("dollarExpressWeightIcon.xlsx");
    return excelNameList;
  }

  @Override
  protected DollarExpressWeightIconConfigCfg createNewBean() {
    return new DollarExpressWeightIconConfigCfg();
  }
}
