package com.jjg.game.dollarexpress.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.dollarexpress.sample.bean.DollarExpressShowCfg;

/**
 * dollarExpressShow.xlsx配置管理容器
 *
 * @excelName dollarExpressShow.xlsx
 * @sheetName DollarExpressShow
 * @author auto_generator
 * @date 2025年06月24日 16:33:11
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DollarExpressShowCfgContainer extends BaseCfgContainer<DollarExpressShowCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public DollarExpressShowCfgContainer getNewContainer(){
    return new DollarExpressShowCfgContainer();
  }

  public DollarExpressShowCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("dollarExpressShow.xlsx");
    return excelNameList;
  }

  @Override
  protected DollarExpressShowCfg createNewBean() {
    return new DollarExpressShowCfg();
  }
}
