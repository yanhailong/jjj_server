package com.jjg.game.sampledata.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Generated;
import com.jjg.game.sampledata.bean.ItemCfg;

/**
 * Item.xlsx配置管理容器
 *
 * @excelName Item.xlsx
 * @sheetName Item
 * @author auto_generator
 * @date 2025年08月15日 18:30:10
 */
@Generated("com.eouna.configtool.generator.template.java.JavaTemplateGenerator")
public class ItemCfgContainer extends BaseCfgContainer<ItemCfg> {

  @Override
  public boolean hasRelatedTable() {
    return false;
  }

  @Override
  public boolean isParentConfigNode() {
    return false;
  }

  @Override
  public ItemCfgContainer getNewContainer(){
    return new ItemCfgContainer();
  }

  public ItemCfgContainer() {
    super();
  }

  @Override
  public List<String> getExcelNameList() {
    List<String> excelNameList = new ArrayList<>();
    excelNameList.add("Item.xlsx");
    return excelNameList;
  }

  @Override
  protected ItemCfg createNewBean() {
    return new ItemCfg();
  }
}
