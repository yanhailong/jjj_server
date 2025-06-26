package com.jjg.game.dollarexpress.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.dollarexpress.sample.bean.DollarExpressLineCfg;

/**
 * dollarExpressLine.xlsx配置管理容器
 *
 * @excelName dollarExpressLine.xlsx
 * @sheetName DollarExpressLine
 * @author auto_generator
 * @date 2025年06月24日 16:33:11
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DollarExpressLineCfgContainer extends BaseCfgContainer<DollarExpressLineCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public DollarExpressLineCfgContainer getNewContainer(){
    return new DollarExpressLineCfgContainer();
  }

  public DollarExpressLineCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("dollarExpressLine.xlsx");
    return excelNameList;
  }

  @Override
  protected DollarExpressLineCfg createNewBean() {
    return new DollarExpressLineCfg();
  }
}
