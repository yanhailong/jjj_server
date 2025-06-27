package com.jjg.game.slots.game.dollarexpress.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.slots.game.dollarexpress.sample.bean.DollarExpressResultShowConfigCfg;

/**
 * dollarExpressResultShow.xlsx配置管理容器
 *
 * @excelName dollarExpressResultShow.xlsx
 * @sheetName DollarExpressResultShowConfig
 * @author auto_generator
 * @date 2025年06月27日 09:57:50
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DollarExpressResultShowConfigCfgContainer extends BaseCfgContainer<DollarExpressResultShowConfigCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public DollarExpressResultShowConfigCfgContainer getNewContainer(){
    return new DollarExpressResultShowConfigCfgContainer();
  }

  public DollarExpressResultShowConfigCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("dollarExpressResultShow.xlsx");
    return excelNameList;
  }

  @Override
  protected DollarExpressResultShowConfigCfg createNewBean() {
    return new DollarExpressResultShowConfigCfg();
  }
}
