package com.jjg.game.hall.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.hall.sample.bean.AllWareHouseConfigCfg;

/**
 * allWareHouse.xlsx配置管理容器
 *
 * @excelName allWareHouse.xlsx
 * @sheetName AllWareHouseConfig
 * @author auto_generator
 * @date 2025年06月27日 16:42:37
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class AllWareHouseConfigCfgContainer extends BaseCfgContainer<AllWareHouseConfigCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public AllWareHouseConfigCfgContainer getNewContainer(){
    return new AllWareHouseConfigCfgContainer();
  }

  public AllWareHouseConfigCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("allWareHouse.xlsx");
    return excelNameList;
  }

  @Override
  protected AllWareHouseConfigCfg createNewBean() {
    return new AllWareHouseConfigCfg();
  }
}
