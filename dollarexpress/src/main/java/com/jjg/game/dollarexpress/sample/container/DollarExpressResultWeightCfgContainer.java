package com.jjg.game.dollarexpress.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.dollarexpress.sample.bean.DollarExpressResultWeightCfg;

/**
 * dollarExpressResultWeight.xlsx配置管理容器
 *
 * @excelName dollarExpressResultWeight.xlsx
 * @sheetName DollarExpressResultWeight
 * @author auto_generator
 * @date 2025年06月24日 16:33:11
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DollarExpressResultWeightCfgContainer extends BaseCfgContainer<DollarExpressResultWeightCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public DollarExpressResultWeightCfgContainer getNewContainer(){
    return new DollarExpressResultWeightCfgContainer();
  }

  public DollarExpressResultWeightCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("dollarExpressResultWeight.xlsx");
    return excelNameList;
  }

  @Override
  protected DollarExpressResultWeightCfg createNewBean() {
    return new DollarExpressResultWeightCfg();
  }
}
