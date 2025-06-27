package com.jjg.game.slots.game.dollarexpress.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.slots.game.dollarexpress.sample.bean.DollarExpressWareHouseCfg;

/**
 * dollarExpressWareHouse.xlsx配置管理容器
 *
 * @excelName dollarExpressWareHouse.xlsx
 * @sheetName DollarExpressWareHouse
 * @author auto_generator
 * @date 2025年06月27日 09:57:50
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class DollarExpressWareHouseCfgContainer extends BaseCfgContainer<DollarExpressWareHouseCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public DollarExpressWareHouseCfgContainer getNewContainer(){
    return new DollarExpressWareHouseCfgContainer();
  }

  public DollarExpressWareHouseCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("dollarExpressWareHouse.xlsx");
    return excelNameList;
  }

  @Override
  protected DollarExpressWareHouseCfg createNewBean() {
    return new DollarExpressWareHouseCfg();
  }
}
