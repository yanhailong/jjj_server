package com.jjg.game.dollarexpress.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.dollarexpress.sample.bean.DollarExpressControlCfg;

/**
 * dollarExpressControl.xlsx配置管理容器
 *
 * @excelName dollarExpressControl.xlsx
 * @sheetName DollarExpressControl
 * @author auto_generator
 * @date 2025年06月24日 16:33:11
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DollarExpressControlCfgContainer extends BaseCfgContainer<DollarExpressControlCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public DollarExpressControlCfgContainer getNewContainer(){
    return new DollarExpressControlCfgContainer();
  }

  public DollarExpressControlCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("dollarExpressControl.xlsx");
    return excelNameList;
  }

  @Override
  protected DollarExpressControlCfg createNewBean() {
    return new DollarExpressControlCfg();
  }
}
