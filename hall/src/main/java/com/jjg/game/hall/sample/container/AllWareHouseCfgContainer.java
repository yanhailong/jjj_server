package com.jjg.game.hall.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.hall.sample.bean.AllWareHouseCfg;

/**
 * allWareHouse.xlsx配置管理容器
 *
 * @excelName allWareHouse.xlsx
 * @sheetName AllWareHouse
 * @author auto_generator
 * @date 2025年06月30日 14:12:12
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class AllWareHouseCfgContainer extends BaseCfgContainer<AllWareHouseCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public AllWareHouseCfgContainer getNewContainer(){
    return new AllWareHouseCfgContainer();
  }

  public AllWareHouseCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("allWareHouse.xlsx");
    return excelNameList;
  }

  @Override
  protected AllWareHouseCfg createNewBean() {
    return new AllWareHouseCfg();
  }
}
