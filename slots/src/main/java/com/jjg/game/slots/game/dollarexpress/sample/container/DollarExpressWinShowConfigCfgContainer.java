package com.jjg.game.slots.game.dollarexpress.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.slots.game.dollarexpress.sample.bean.DollarExpressWinShowConfigCfg;

/**
 * dollarExpressWinShow.xlsx配置管理容器
 *
 * @excelName dollarExpressWinShow.xlsx
 * @sheetName DollarExpressWinShowConfig
 * @author auto_generator
 * @date 2025年06月27日 09:57:50
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DollarExpressWinShowConfigCfgContainer extends BaseCfgContainer<DollarExpressWinShowConfigCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public DollarExpressWinShowConfigCfgContainer getNewContainer(){
    return new DollarExpressWinShowConfigCfgContainer();
  }

  public DollarExpressWinShowConfigCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("dollarExpressWinShow.xlsx");
    return excelNameList;
  }

  @Override
  protected DollarExpressWinShowConfigCfg createNewBean() {
    return new DollarExpressWinShowConfigCfg();
  }
}
