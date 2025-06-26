package com.jjg.game.dollarexpress.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.dollarexpress.sample.bean.DollarExpressIconCfg;

/**
 * dollarExpressIcon.xlsx配置管理容器
 *
 * @excelName dollarExpressIcon.xlsx
 * @sheetName DollarExpressIcon
 * @author auto_generator
 * @date 2025年06月24日 16:33:11
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DollarExpressIconCfgContainer extends BaseCfgContainer<DollarExpressIconCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public DollarExpressIconCfgContainer getNewContainer(){
    return new DollarExpressIconCfgContainer();
  }

  public DollarExpressIconCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("dollarExpressIcon.xlsx");
    return excelNameList;
  }

  @Override
  protected DollarExpressIconCfg createNewBean() {
    return new DollarExpressIconCfg();
  }
}
