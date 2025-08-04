package com.jjg.game.room.sample.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.room.sample.bean.WarehouseCfg;

/**
 * warehouse.xlsx配置管理容器
 *
 * @excelName warehouse.xlsx
 * @sheetName Warehouse
 * @author auto_generator
 * @date 2025年08月04日 10:01:24
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class WarehouseCfgContainer extends BaseCfgContainer<WarehouseCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public WarehouseCfgContainer getNewContainer(){
    return new WarehouseCfgContainer();
  }

  public WarehouseCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("warehouse.xlsx");
    return excelNameList;
  }

  @Override
  protected WarehouseCfg createNewBean() {
    return new WarehouseCfg();
  }
}
